package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class ÅrsavregningTest {

    @Test
    fun `beregnTilFaktureringsBeloep uten tidligere fakturert beløp benytter totalbeløp`() {
        val årsavregning = Årsavregning().apply {
            nyttTotalbeloep = BigDecimal(1000)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(1000)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp fra avgiftssystemet trekker fra tidligere fakturert beløp fra avgiftssystemet`() {
        val årsavregning = Årsavregning().apply {
            nyttTotalbeloep = BigDecimal(1000)
            harDeltGrunnlag = true
            tidligereFakturertBeloepAvgiftssystem = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp trekker fra tidligere fakturert beløp`() {
        val årsavregning = Årsavregning().apply {
            nyttTotalbeloep = BigDecimal(1000)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp og tidligere fakturert beløp fra avgiftssystemet trekker fra begge`() {
        val årsavregning = Årsavregning().apply {
            nyttTotalbeloep = BigDecimal(1000)
            harDeltGrunnlag = true
            tidligereFakturertBeloepAvgiftssystem = BigDecimal(200)
            tidligereFakturertBeloep = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(600)
    }
}
