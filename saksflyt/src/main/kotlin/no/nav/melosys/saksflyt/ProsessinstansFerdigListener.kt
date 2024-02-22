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
        if (prosessinstansFerdigEvent.låsReferanse == null) {
            log.info("Prosessinstans ${prosessinstansFerdigEvent.uuid} ferdig uten låsreferanse")
            return
        }

        log.info("Prosessinstans ${prosessinstansFerdigEvent.uuid} ferdig, sjekker om neste med låsreferanse:${prosessinstansFerdigEvent.låsReferanse} kan startestes")
        if (kanNesteProsessinstansStartes(prosessinstansFerdigEvent)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun kanNesteProsessinstansStartes(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean {
        if (!prosessinstansRepository.existsByStatusNotInAndLåsReferanse(setOf(ProsessStatus.FERDIG), prosessinstansFerdigEvent.låsReferanse)) {
            log.info("Det finnes ingen aktiv prosessinstans med låsreferanse ${prosessinstansFerdigEvent.låsReferanse}")
            return true
        }

        return finnesProssesserMedSammeLåsReferanseOgForskjelligIdpåVent(prosessinstansFerdigEvent)
    }

    private fun finnesProssesserMedSammeLåsReferanseOgForskjelligIdpåVent(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean =
        prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            prosessinstansFerdigEvent.uuid,
            setOf(ProsessStatus.FERDIG),
            prosessinstansFerdigEvent.låsReferanse
        ).filter { it.prosessStatus == ProsessStatus.PÅ_VENT && it.låsReferanse == prosessinstansFerdigEvent.låsReferanse }.apply {
            log.info("$size prosessinstans(er) med nøyaktig samme låsreferanse ${prosessinstansFerdigEvent.låsReferanse} er på vent")
        }.apply {
            if (isEmpty()) log.info("Ingen på vent med nøyaktig samme låsreferanse ${prosessinstansFerdigEvent.låsReferanse}")
        }.isNotEmpty()

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        val allePåVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
        val antallPåVentMedSammeReferanse =
            allePåVent.count { LåsReferanseFactory.harSammeReferanse(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse) }

        val påVent = allePåVent.firstOrNull { it.låsReferanse == prosessinstansFerdigEvent.låsReferanse } // ta sub-prosesser først
            ?: allePåVent
                .filter { LåsReferanseFactory.harSammeReferanse(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse) }
                .sortedBy { it.registrertDato }
                .firstOrNull()

        log.info("$antallPåVentMedSammeReferanse på vent, neste som kan kjøres er ${påVent?.låsReferanse} for ferdig låsreferanse ${prosessinstansFerdigEvent.låsReferanse}")

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

