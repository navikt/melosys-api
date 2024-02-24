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

        log.info("Prosessinstans ${prosessinstansFerdigEvent.uuid} ferdig, sjekker om neste med låsreferanse:${prosessinstansFerdigEvent.låsReferanse} kan startes")
        if (kanNesteProsessinstansStartes(prosessinstansFerdigEvent)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun kanNesteProsessinstansStartes(prosessinstansFerdigEvent: ProsessinstansFerdigEvent): Boolean =
        prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT).filter {
            LåsReferanseFactory.harSammeGruppePrefiks(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse)
        }.apply {
            log.info("Prosessinstans(er) på vent med samme gruppe-prefiks: ${this.map { it.id }}")
        }.isNotEmpty()

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        val allePåVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)

        val påVent = allePåVent.filter { it.låsReferanse == prosessinstansFerdigEvent.låsReferanse } // ta sub-prosesser først
            .sortedByDescending { it.registrertDato } // sub-prosesser har mest sannsynlig blitt opprettet etter hovedprosessene
            .firstOrNull()
            ?: allePåVent
                .filter { LåsReferanseFactory.harSammeGruppePrefiks(it.låsReferanse, prosessinstansFerdigEvent.låsReferanse) }
                .sortedBy { it.registrertDato }
                .firstOrNull()

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

