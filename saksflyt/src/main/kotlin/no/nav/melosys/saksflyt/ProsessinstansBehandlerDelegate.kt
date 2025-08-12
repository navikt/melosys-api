package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.LåsReferanse
import no.nav.melosys.saksflytapi.domain.LåsReferanseFactory
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

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
        2. Det finnes ingen aktiv prosessinstans med samme gruppe prefiks
     */
    private fun skalSettesPåVent(prosessinstans: Prosessinstans): Boolean {
        if (prosessinstans.låsReferanse == null) {
            return false
        }

        val låsReferanse: LåsReferanse = LåsReferanseFactory.lagLåsReferanse(prosessinstans.hentLåsReferanse)
        val andreAktiveLåsMedSammeGruppePrefiks = finnAndreAktiveLåsMedSammegruppePrefiks(prosessinstans.id!!, låsReferanse.gruppePrefiks)
        log.info { "Låsreferanse: ${prosessinstans.låsReferanse} Andre aktive med samme gruppe prefiks: $andreAktiveLåsMedSammeGruppePrefiks" }
        return låsReferanse.skalSettesPåVent(andreAktiveLåsMedSammeGruppePrefiks)
    }

    internal fun finnAndreAktiveLåsMedSammegruppePrefiks(id: UUID, låsReferanseStarterMed: String): Collection<String> =
        prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
            id, setOf(ProsessStatus.PÅ_VENT, ProsessStatus.FERDIG), låsReferanseStarterMed
        ).map { it.låsReferanse }
}
