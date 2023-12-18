package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.SedLåsReferanse
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
        if (prosessinstansFerdigEvent.låsReferanse != null && !finnesAktivReferanse(prosessinstansFerdigEvent.låsReferanse)) {
            startNesteProsessinstans(prosessinstansFerdigEvent)
        }
    }

    private fun finnesAktivReferanse(referanse: String): Boolean =
        prosessinstansRepository.existsByStatusNotInAndLåsReferanse(setOf(ProsessStatus.FERDIG), referanse)

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info("Forsøker å starte neste prosessinstans, låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
        val ferdigReferanse = SedLåsReferanse(prosessinstansFerdigEvent.låsReferanse)

        prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)
            .filter { harSammeReferanse(it, ferdigReferanse) }
            .sortedBy { it.registrertDato }
            .firstOrNull()
            ?.let { oppdaterStatusOgBehandleProsessinstans(it) }
    }

    private fun oppdaterStatusOgBehandleProsessinstans(prosessinstans: Prosessinstans) {
        log.info("Prosessinstans {} startes opp etter å ha vært på vent", prosessinstans.id)
        prosessinstans.status = ProsessStatus.KLAR
        prosessinstans.endretDato = LocalDateTime.now()
        prosessinstansRepository.save(prosessinstans)
        prosessinstansBehandler.behandleProsessinstans(prosessinstans)
    }

    private fun harSammeReferanse(prosessinstans: Prosessinstans, ferdigLåsreferanse: SedLåsReferanse): Boolean {
        val låsReferanse = SedLåsReferanse(prosessinstans.låsReferanse)
        // referanse er her rina saksnummer så må støtte dette når vi lager felles interface
        return låsReferanse.referanse == ferdigLåsreferanse.referanse
    }
}
