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
        log.info("Prosessinstans {} ferdig", prosessinstansFerdigEvent.uuid)
        if (prosessinstansFerdigEvent.låsReferanse != null && finnesIkkeAktivReferanse(prosessinstansFerdigEvent)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun finnesIkkeAktivReferanse(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean {
        if (!prosessinstansRepository.existsByStatusNotInAndLåsReferanse(setOf(ProsessStatus.FERDIG), prosessinstansFerdigEvent.låsReferanse)) {
            log.info("Det finnes ingen aktiv prosessinstans med låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
            return true
        }
        log.info("Det finnes en aktiv prosessinstans med låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
        return finnesProssesserMedSammeLåsReferanseOgForskjelligIdpåVent(prosessinstansFerdigEvent)
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
        log.info("Forsøker å starte neste prosessinstans, låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
        val ferdigReferanse = LåsReferanseFactory.lagLåsReferanse(prosessinstansFerdigEvent.låsReferanse)

        prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .filter { harSammeReferanse(it, ferdigReferanse) }
            .sortedBy { it.registrertDato }
            .firstOrNull()
            ?.let { oppdaterStatusOgBehandleProsessinstans(it) }
    }

    private fun oppdaterStatusOgBehandleProsessinstans(prosessinstans: Prosessinstans) {
        log.info("Prosessinstans {} med låsreferanse {} startes opp etter å ha vært på vent", prosessinstans.id, prosessinstans.låsReferanse)
        prosessinstans.status = ProsessStatus.KLAR
        prosessinstans.endretDato = LocalDateTime.now()
        prosessinstansRepository.save(prosessinstans)
        prosessinstansBehandler.behandleProsessinstans(prosessinstans)
    }

    private fun harSammeReferanse(prosessinstans: Prosessinstans, ferdigLåsreferanse: LåsReferanse): Boolean {
        val låsReferanse = LåsReferanseFactory.lagLåsReferanse(prosessinstans.låsReferanse)
        return låsReferanse.referanse == ferdigLåsreferanse.referanse
    }
}
