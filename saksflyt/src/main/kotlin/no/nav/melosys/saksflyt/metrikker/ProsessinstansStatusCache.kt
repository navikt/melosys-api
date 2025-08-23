package no.nav.melosys.saksflyt.metrikker

import mu.KotlinLogging
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansStatusCache(private val prosessinstansRepository: ProsessinstansRepository) {

    private val antallPerTypeOgStatus = mutableMapOf<Pair<ProsessType, ProsessStatus>, Long>()
    private val antallPerStegOgStatus = mutableMapOf<Pair<ProsessSteg, ProsessStatus>, Long>()


    fun antallProsessinstanserFeiletPåType(type: ProsessType): Double =
        antallProsessinstanserMedTypeOgStatus(type, STATUS_FEILET).toDouble()

    fun antallProsessinstanserFeiletPåSteg(prosessSteg: ProsessSteg): Double =
        antallProsessinstanserMedStegOgStatus(prosessSteg, STATUS_FEILET).toDouble()

    private fun antallProsessinstanserMedStegOgStatus(
        prosessSteg: ProsessSteg,
        statuser: Set<ProsessStatus>
    ): Long = statuser.sumOf { status -> antallPerStegOgStatus[prosessSteg to status] ?: 0 }

    private fun antallProsessinstanserMedTypeOgStatus(
        prosessType: ProsessType,
        statuser: Set<ProsessStatus>
    ): Long = statuser.sumOf { status -> antallPerTypeOgStatus[prosessType to status] ?: 0 }

    @Scheduled(fixedRateString = "\${melosys.prosesser.status.oppfriskning.frekvens:30000}")
    private fun oppfriskCache() {
        log.debug("Oppfrisker caching av metrikker for prosessinstanser")
        val tidStart = System.currentTimeMillis()

        oppfriskPerTypeOgStatus()
        oppfriskPerStegOgStatus()

        val tidBrukt = System.currentTimeMillis() - tidStart
        log.debug("Oppfriskning av cache av metrikker for prosessinstanser tok {} millisekunder.", tidBrukt)
    }

    private fun oppfriskPerTypeOgStatus() {
        val prosessinstansMetrikker = prosessinstansRepository
            .antallAktiveOgFeiletPerTypeOgStatus(PROSESS_TYPER).toList()

        antallPerTypeOgStatus.clear()
        prosessinstansMetrikker.forEach { prosessinstansAntall ->
            antallPerTypeOgStatus[prosessinstansAntall.prosessType to prosessinstansAntall.prosessStatus] =
                prosessinstansAntall.antall
        }
    }

    private fun oppfriskPerStegOgStatus() {
        val prosessinstansMetrikkerForStegOgStatus = prosessinstansRepository
            .antallAktiveOgFeiletPerStegOgStatus(PROSESS_STEG, true).toList()

        antallPerStegOgStatus.clear()
        prosessinstansMetrikkerForStegOgStatus.forEach { prosessinstansStegAntall ->
            val feiletSteg = ProsessflytDefinisjon.hentNesteSteg(
                prosessinstansStegAntall.prosessType, prosessinstansStegAntall.sistFullfortSteg
            ).orElse(null)

            feiletSteg?.let {
                val stegOgStatus = it to prosessinstansStegAntall.prosessStatus
                antallPerStegOgStatus[stegOgStatus] =
                    (antallPerStegOgStatus[stegOgStatus] ?: 0) + prosessinstansStegAntall.antall
            }
        }
    }

    companion object {
        private val PROSESS_TYPER = ProsessType.values().toSet()
        private val PROSESS_STEG = ProsessSteg.values().toSet()
        private val STATUS_FEILET = setOf(ProsessStatus.FEILET)
    }
}
