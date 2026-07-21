package no.nav.melosys.saksflyt.prosessflyt

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test

internal class ProsessflytDefinisjonTest {

    /**
     * Arkitekturregel: enhver flyt som avslutter sak/behandling skal synkronisere saksstatus til
     * melosys-skjema-api som SISTE steg (AvsluttFagsakOgBehandling-steget bruker
     * HÅNDTERES_AV_PROSESSFLYT og er avhengig av at flyten selv eier synk-steget; steget ligger
     * sist slik at en synk-feil ikke stopper forretningskritiske steg). Uten denne regelen kan en
     * fremtidig flyt stille glemme steget, og skjema-api vil vise utdatert status til innsender.
     */
    @Test
    fun `alle flyter med AVSLUTT_SAK_OG_BEHANDLING har SYNK_SKJEMA_SAKSSTATUS som siste steg`() {
        val flyterMedAvslutt = ProsessType.entries
            .mapNotNull { type -> hentStegListe(type)?.let { type to it } }
            .filter { (_, steg) -> ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING in steg }

        withClue("Fant ingen flyter med AVSLUTT_SAK_OG_BEHANDLING — testen er feilkonfigurert") {
            flyterMedAvslutt.isNotEmpty() shouldBe true
        }

        flyterMedAvslutt.forEach { (type, steg) ->
            withClue("Flyten $type avslutter sak/behandling, men mangler SYNK_SKJEMA_SAKSSTATUS som siste steg") {
                steg.last() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
            }
        }
    }

    @Test
    fun `MOTTAK_SED har SYNK_SKJEMA_SAKSSTATUS som siste steg`() {
        // SED-rutingen annullerer saker og markerer instansen med SYNK_SAKSSTATUS_SAKSNUMMER
        hentStegListe(ProsessType.MOTTAK_SED)!!.last() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
    }

    @Test
    fun `digital-søknad-mottaksflytene har SYNK_SKJEMA_SAKSSTATUS som siste steg`() {
        // Mottak (særlig på eksisterende sak) endrer ikke fagsakstatus — synken må trigges av flyten
        hentStegListe(ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD)!!.last() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
        hentStegListe(ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD)!!.last() shouldBe ProsessSteg.SYNK_SKJEMA_SAKSSTATUS
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
