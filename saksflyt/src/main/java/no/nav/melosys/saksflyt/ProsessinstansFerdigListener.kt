package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.SedLåsReferanse
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Set

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

    private fun finnesAktivReferanse(referanse: String): Boolean {
        return prosessinstansRepository.existsByStatusNotInAndLåsReferanse(Set.of(ProsessStatus.FERDIG), referanse)
    }

    private fun startNesteProsessinstans(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info("Forsøker å starte neste prosessinstans, låsreferanse {}", prosessinstansFerdigEvent.låsReferanse)
        val ferdigReferanse = SedLåsReferanse(prosessinstansFerdigEvent.låsReferanse)

        val prosessinstanserPåVent = prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)

        prosessinstanserPåVent.stream()
            .filter { p: Prosessinstans -> harSammeReferanse(p, ferdigReferanse) }
            .min(Comparator.comparing { obj: Prosessinstans -> obj.registrertDato })
            .ifPresent { prosessinstans: Prosessinstans -> this.oppdaterStatusOgBehandleProsessinstans(prosessinstans) }
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
        return låsReferanse.referanse == ferdigLåsreferanse.referanse
    }
}
