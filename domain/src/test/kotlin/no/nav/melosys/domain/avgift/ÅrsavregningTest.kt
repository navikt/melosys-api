package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ÅrsavregningTest {

    @Test
    fun `beregnTilFaktureringsBeloep uten tidligere fakturert beløp benytter totalbeløp`() {
        val årsavregning = Årsavregning().apply {
            beregnetAvgiftBelop = BigDecimal(1000)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(1000)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp fra avgiftssystemet trekker fra tidligere fakturert beløp fra avgiftssystemet`() {
        val årsavregning = Årsavregning().apply {
            beregnetAvgiftBelop = BigDecimal(1000)
            harTrygdeavgiftFraAvgiftssystemet = true
            trygdeavgiftFraAvgiftssystemet = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp trekker fra tidligere fakturert beløp`() {
        val årsavregning = Årsavregning().apply {
            beregnetAvgiftBelop = BigDecimal(1000)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp og tidligere fakturert beløp fra avgiftssystemet trekker fra begge`() {
        val årsavregning = Årsavregning().apply {
            beregnetAvgiftBelop = BigDecimal(1000)
            harTrygdeavgiftFraAvgiftssystemet = true
            trygdeavgiftFraAvgiftssystemet = BigDecimal(200)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(600)
    }
}
