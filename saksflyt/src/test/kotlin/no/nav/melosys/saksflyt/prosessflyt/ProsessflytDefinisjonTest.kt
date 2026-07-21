package no.nav.melosys.saksflyt.prosessflyt

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test

internal class ProsessflytDefinisjonTest {

    /**
     * Arkitekturregel: enhver flyt som avslutter sak/behandling skal synkronisere saksstatus til
     * melosys-skjema-api ETTERPÅ (AvsluttFagsakOgBehandling-steget bruker HÅNDTERES_AV_PROSESSFLYT
     * og er avhengig av at flyten selv eier synk-steget). Uten denne regelen kan en fremtidig flyt
     * stille glemme steget, og skjema-api vil vise utdatert status til innsender.
     */
    @Test
    fun `alle flyter med AVSLUTT_SAK_OG_BEHANDLING har SYNK_SKJEMA_SAKSSTATUS rett etterpå`() {
        val flyterMedAvslutt = ProsessType.entries
            .mapNotNull { type -> hentStegListe(type)?.let { type to it } }
            .filter { (_, steg) -> ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING in steg }

        withClue("Fant ingen flyter med AVSLUTT_SAK_OG_BEHANDLING — testen er feilkonfigurert") {
            flyterMedAvslutt.isNotEmpty() shouldBe true
        }

        flyterMedAvslutt.forEach { (type, steg) ->
            val etterAvslutt = steg.getOrNull(steg.indexOf(ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING) + 1)
            withClue("Flyten $type avslutter sak/behandling, men mangler SYNK_SKJEMA_SAKSSTATUS rett etter AVSLUTT_SAK_OG_BEHANDLING") {
                etterAvslutt shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
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
