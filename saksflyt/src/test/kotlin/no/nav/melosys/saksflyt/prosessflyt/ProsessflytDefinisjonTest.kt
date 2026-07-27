package no.nav.melosys.saksflyt.prosessflyt

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test

internal class ProsessflytDefinisjonTest {

    /**
     * Steg som setter SYNK_SAKSSTATUS_SAKSNUMMER-markøren (via
     * Prosessinstans.markerForSkjemaSaksstatusSynk) fordi de endrer fagsakstatus. Legger et steg
     * til her når det begynner å sette markøren — testen under håndhever da at flytene det inngår
     * i får synk-steget sist.
     */
    private val markørSettendeSteg = listOf(
        ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING,
        ProsessSteg.SED_MOTTAK_RUTING
    )

    /**
     * Arkitekturregel: enhver flyt som inneholder et markør-settende steg skal synkronisere
     * saksstatus til melosys-skjema-api som SISTE steg. Stegene bruker
     * HÅNDTERES_AV_PROSESSFLYT/markør-idiomet og er avhengige av at flyten selv eier synk-steget;
     * steget ligger sist slik at en synk-feil ikke stopper forretningskritiske steg. Uten denne
     * regelen kan en fremtidig flyt stille glemme steget, og skjema-api vil vise utdatert status
     * til innsender.
     */
    @Test
    fun `alle flyter med markør-settende steg har SYNK_SKJEMA_SAKSSTATUS som siste steg`() {
        val flyterMedMarkørSettendeSteg = ProsessType.entries
            .mapNotNull { type -> hentStegListe(type)?.let { type to it } }
            .filter { (_, steg) -> steg.any { it in markørSettendeSteg } }

        withClue("Fant ingen flyter med markør-settende steg — testen er feilkonfigurert") {
            flyterMedMarkørSettendeSteg.isNotEmpty() shouldBe true
        }

        flyterMedMarkørSettendeSteg.forEach { (type, steg) ->
            withClue(
                "Flyten $type inneholder et steg som kan markere for saksstatus-synk " +
                    "(${steg.filter { it in markørSettendeSteg }}), men mangler SYNK_SKJEMA_SAKSSTATUS som siste steg"
            ) {
                steg.last() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
            }
        }
    }

    /** Bygger flytens stegliste via det offentlige nesteSteg-API-et. */
    private fun hentStegListe(prosessType: ProsessType): List<ProsessSteg>? {
        val flyt = ProsessflytDefinisjon.finnFlytForProsessType(prosessType).orElse(null) ?: return null
        val steg = mutableListOf<ProsessSteg>()
        var neste = flyt.nesteSteg(null)
        while (neste != null) {
            steg += neste
            neste = flyt.nesteSteg(neste)
        }
        return steg
    }
}
