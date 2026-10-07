package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ÅrsavregningTest {

    @Test
    fun `beregnTilFaktureringsBeloep uten tidligere fakturert beløp benytter totalbeløp`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(1000)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp fra avgiftssystemet trekker fra tidligere fakturert beløp fra avgiftssystemet`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
            harInnbetaltTrygdeavgift = true
            innbetaltTrygdeavgift = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp trekker fra tidligere fakturert beløp`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp og tidligere fakturert beløp fra avgiftssystemet trekker fra begge`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
            harInnbetaltTrygdeavgift = true
            innbetaltTrygdeavgift = BigDecimal(200)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(600)
    }

    @Test
    fun `beregnTilFaktureringsBeloep legger tilbake innbetalt fra tidligere årsavregning`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
            harInnbetaltTrygdeavgift = true
            innbetaltTrygdeavgift = BigDecimal(300)
            tidligereFakturertBeloep = BigDecimal(1500)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = BigDecimal(300))

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(-500)
    }

    @Test
    fun `beregnTilFaktureringsBeloep uten endelig avgift nuller gammelt beløp til fakturering`() {
        val årsavregning = Årsavregning.forTest {
            tidligereFakturertBeloep = BigDecimal(1500)
            tilFaktureringBeloep = BigDecimal(-500)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)

        årsavregning.tilFaktureringBeloep shouldBe null
    }

    @Test
    fun `tilbakelagtInnbetalt er beløpet som ble lagt tilbake, eller null når ingenting ble lagt tilbake`() {
        val årsavregning = Årsavregning.forTest {
            manueltAvgiftBeloep = BigDecimal(1000)
            innbetaltTrygdeavgift = BigDecimal(300)
            tidligereFakturertBeloep = BigDecimal(1500)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = BigDecimal(300))
        årsavregning.tilbakelagtInnbetalt shouldBe BigDecimal(300)

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = BigDecimal.ZERO)
        årsavregning.tilbakelagtInnbetalt shouldBe null

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = null)
        årsavregning.tilbakelagtInnbetalt shouldBe null
    }

    @Test
    fun `hentTidligereBetaltTotalt er endelig avgift minus beløp til fakturering, uten dobbelt innbetalt`() {
        val årsavregning = Årsavregning.forTest {
            beregnetAvgiftBelop = BigDecimal(1000)
            harInnbetaltTrygdeavgift = true
            innbetaltTrygdeavgift = BigDecimal(300)
            tidligereFakturertBeloep = BigDecimal(1500)
        }

        årsavregning.beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt = BigDecimal(300))

        // 1500 + 300 - 300: innbetalingen inngikk allerede i de 1500
        årsavregning.hentTidligereBetaltTotalt shouldBe BigDecimal(1500)
    }
}
