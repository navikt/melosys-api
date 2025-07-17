package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsresultat
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class ÅrsavregningTest {

    private fun nyÅrsavregning(init: Årsavregning.() -> Unit = {}): Årsavregning {
        return Årsavregning(
            id = 1L,
            behandlingsresultat = Behandlingsresultat(),
            aar = 2023
        ).apply(init)
    }

    @Test
    fun `beregnTilFaktureringsBeloep uten tidligere fakturert beløp benytter totalbeløp`() {
        val årsavregning = nyÅrsavregning {
            beregnetAvgiftBelop = BigDecimal(1000)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(1000)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp fra avgiftssystemet`() {
        val årsavregning = nyÅrsavregning {
            beregnetAvgiftBelop = BigDecimal(1000)
            trygdeavgiftFraAvgiftssystemet = BigDecimal(200)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(800)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert beløp`() {
        val årsavregning = nyÅrsavregning {
            beregnetAvgiftBelop = BigDecimal(1000)
            tidligereFakturertBeloep = BigDecimal(300)
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(700)
    }

    @Test
    fun `beregnTilFaktureringsBeloep med tidligere fakturert og systembeløp`() {
        val tidligere = nyÅrsavregning {
            trygdeavgiftFraAvgiftssystemet = BigDecimal(100)
        }

        val årsavregning = nyÅrsavregning {
            beregnetAvgiftBelop = BigDecimal(1000)
            tidligereFakturertBeloep = BigDecimal(200)
            trygdeavgiftFraAvgiftssystemet = BigDecimal(150)
            tidligereBehandlingsresultat = Behandlingsresultat().apply {
                årsavregning = tidligere
            }
        }

        årsavregning.beregnTilFaktureringsBeloep()

        årsavregning.tilFaktureringBeloep shouldBe BigDecimal(750)
    }
}
