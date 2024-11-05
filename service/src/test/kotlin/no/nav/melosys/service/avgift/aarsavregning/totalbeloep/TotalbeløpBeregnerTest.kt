package no.nav.melosys.service.avgift.aarsavregning.totalbeloep

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TotalbeløpBeregnerTest {

    private val enhetspris = BigDecimal("1000.00")

    @Test
    fun regnForPeriode_januar_sisteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 1, 31)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("350.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_november_førsteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 11, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 12, 31)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("11350.00")
        result.shouldBe(forventetBeløp)
    }


    @Test
    fun regnForPeriode_januarTilNovember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("10500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_februarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 2, 14)
        val tom = LocalDate.of(2023, 12, 31)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("10540.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilHalveFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 15)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("13520.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("14000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2022TilMars2024_regnesKorrekt() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2024, 3, 31)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("27000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_skuddÅrFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2024, 2, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("1000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_midtenAvDesember2023TilMidtenAvFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 12, 14)
        val tom = LocalDate.of(2024, 2, 15)


        val result = TotalbeløpBeregner.totalBeløpForPeriode(fom, tom, enhetspris)


        val forventetBeløp = BigDecimal("2100.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun `Totalbeløp, En periode for hele måneder`() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 5, 31)
        val periodeMedBeløp = PeriodeMedBeløp(fom, tom, BigDecimal.valueOf(700))

        val result = TotalbeløpBeregner.totalBeløpForAllePerioder(listOf(periodeMedBeløp))

        val forventetBeløp = BigDecimal("3500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun `Totalbeløp, Ulike perioder med ulik beløp`() {
        val fom = LocalDate.of(2023, 1, 13)
        val tom = LocalDate.of(2023, 12, 31)
        val periode1 = PeriodeMedBeløp(fom, tom, BigDecimal.valueOf(500))
        val fom2 = LocalDate.of(2023, 6, 1)
        val tom2 = LocalDate.of(2023, 12, 15)
        val periode2 = PeriodeMedBeløp(fom2, tom2, BigDecimal.valueOf(1000))

        val result = TotalbeløpBeregner.totalBeløpForAllePerioder(listOf(periode1, periode2))

        val forventetBeløp = BigDecimal("12285.00")
        result.shouldBe(forventetBeløp)
    }
}
