package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

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
            log.info("Prosessinstans {} med låsreferanse {} satt på vent", prosessinstans.id, prosessinstans.låsReferanse)
        }
    }

    /*
    Settes på vent om det finnes en prosessinstans med samme referanse, som ikke er på vent/ferdig.

    Settes ikke på vent om
        1. Prosessinstansen ikke har en låsreferanse
        2. Det finnes ingen prosessinstans med samme referanse
     */
    private fun skalSettesPåVent(prosessinstans: Prosessinstans): Boolean {
        if (prosessinstans.låsReferanse == null) {
            return false
        }

        val låsReferanse: LåsReferanse = LåsReferanseFactory.lagLåsReferanse(prosessinstans.låsReferanse)
        val andreAktiveLåsMedSammeReferanse = finnAndreAktiveLåsMedSammeReferanse(prosessinstans.id, låsReferanse.referanse)
        return låsReferanse.skalSettesPåVent(andreAktiveLåsMedSammeReferanse)
    }

    internal fun finnAndreAktiveLåsMedSammeReferanse(id: UUID, låsReferansePrefix: String): Collection<String> {
        return prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            id, setOf(ProsessStatus.PÅ_VENT, ProsessStatus.FERDIG), låsReferansePrefix
        ).map { it.låsReferanse }.toSet()
    }
}
