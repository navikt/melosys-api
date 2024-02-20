package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.*
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansFerdigListener(
    private val prosessinstansRepository: ProsessinstansRepository,
    private val prosessinstansBehandler: ProsessinstansBehandler
) {
    @EventListener
    fun prosessinstansFerdig(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info("Prosessinstans {} ferdig låsreferanse {}", prosessinstansFerdigEvent.uuid, prosessinstansFerdigEvent.låsReferanse)
        if (prosessinstansFerdigEvent.låsReferanse != null && finnesIkkeAktivReferanse(prosessinstansFerdigEvent)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun finnesIkkeAktivReferanse(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean {
        if (!prosessinstansRepository.existsByStatusNotInAndLåsReferanse(setOf(ProsessStatus.FERDIG), prosessinstansFerdigEvent.låsReferanse)) {
            log.info("Det finnes ingen aktiv prosessinstans med låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
            return true
        }
        log.info("Det finnes en aktiv prosessinstans med låsreferanse ${prosessinstansFerdigEvent.låsReferanse}")
        return finnesProssesserMedSammeLåsReferanseOgForskjelligIdpåVent(prosessinstansFerdigEvent).apply {
            log.info("finnesIkkeAktivReferanse for ${prosessinstansFerdigEvent.låsReferanse} = $this")
        }
    }

    private fun finnesProssesserMedSammeLåsReferanseOgForskjelligIdpåVent(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean =
    // Dette bør ryddes mer i og det er egen oppgave for å fikse sed synkronisering med samme
        // låsreferanse: https://jira.adeo.no/browse/MELOSYS-6365
        prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            prosessinstansFerdigEvent.uuid,
            setOf(ProsessStatus.FERDIG),
            prosessinstansFerdigEvent.låsReferanse
        ).filter { it.prosessStatus == ProsessStatus.PÅ_VENT && it.låsReferanse == prosessinstansFerdigEvent.låsReferanse }.apply {
            log.info("$size prosessinstanser med nøyaktig samme låsreferanse ${prosessinstansFerdigEvent.låsReferanse} er på vent")
        }.isNotEmpty()

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        val count = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .count { LåsReferanseFactory.harSammeReferanse(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse) }

        val påVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .filter { LåsReferanseFactory.harSammeReferanse(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse) }
            .sortedBy { it.registrertDato }
            .firstOrNull()

        log.info("$count på vent, neste som kan kjøres ${påVent?.id} for låsreferanse ${prosessinstansFerdigEvent.låsReferanse}")

        if (påVent != null) {
            oppdaterStatusOgBehandleProsessinstans(påVent)
        }
    }

    private fun oppdaterStatusOgBehandleProsessinstans(prosessinstans: Prosessinstans) {
        log.info("Prosessinstans {} med låsreferanse {} startes opp etter å ha vært på vent", prosessinstans.id, prosessinstans.låsReferanse)
        prosessinstans.status = ProsessStatus.KLAR
        prosessinstans.endretDato = LocalDateTime.now()
        prosessinstansRepository.save(prosessinstans)
        prosessinstansBehandler.behandleProsessinstans(prosessinstans)
    }
}

