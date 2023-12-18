package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessinstansInfo
import no.nav.melosys.saksflytapi.domain.SedLåsReferanse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*
import java.util.Set
import java.util.stream.Collectors

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansBehandlerDelegate(
    private val prosessinstansBehandler: ProsessinstansBehandler,
    private val prosessinstansRepository: ProsessinstansRepository
) {

    fun behandleProsessinstans(prosessinstans: Prosessinstans) {
        oppdaterStatusOmSkalPåVent(prosessinstans)
        if (!prosessinstans.erPåVent()) {
            prosessinstansBehandler.behandleProsessinstans(prosessinstans)
        }
    }

    fun oppdaterStatusOmSkalPåVent(prosessinstans: Prosessinstans) {
        if (skalSettesPåVent(prosessinstans)) {
            prosessinstans.status = ProsessStatus.PÅ_VENT
            prosessinstans.endretDato = LocalDateTime.now()
            prosessinstansRepository.save(prosessinstans)
            log.info("Prosessinstans {} satt på vent", prosessinstans.id)
        }
    }

    /*
    Settes på vent om det finnes en prosessinstans med samme referanse,
     men ikke lik identifikator i prosess (ikke på vent/ferdig).

    Settes ikke på vent om
        1. Prosessinstansen ikke har en låsreferanse
        2. Det finnes ingen prosessinstans med samme referanse
        3. Det finnes en prosessinstans med lik referanse og identifikator.
     */
    private fun skalSettesPåVent(prosessinstans: Prosessinstans): Boolean {
        if (prosessinstans.låsReferanse == null) {
            return false
        }

        val låsReferanse = SedLåsReferanse(prosessinstans.låsReferanse)
        val aktiveLåsReferanser = finnAndreAktiveLåsMedSammeReferanse(prosessinstans.id, låsReferanse)

        if (aktiveLåsReferanser.contains(låsReferanse)) {
            return false
        }
        return aktiveLåsReferanser.stream()
            .anyMatch { sedLåsReferanse: SedLåsReferanse -> sedLåsReferanse.referanse == låsReferanse.referanse }
    }

    private fun finnAndreAktiveLåsMedSammeReferanse(
        id: UUID,
        låsReferanse: SedLåsReferanse
    ): Collection<SedLåsReferanse> {
        return prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            id, Set.of(ProsessStatus.PÅ_VENT, ProsessStatus.FERDIG), låsReferanse.referanse
        )
            .stream()
            .map { p: ProsessinstansInfo -> SedLåsReferanse(p.låsReferanse) }
            .collect(Collectors.toSet())
    }
}
