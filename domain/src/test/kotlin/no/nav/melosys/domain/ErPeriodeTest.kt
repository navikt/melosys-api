package no.nav.melosys.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ErPeriodeTest {

    private class TestPeriode(private val fom: LocalDate?, private val tom: LocalDate?) : ErPeriode {
        override fun getFom() = fom
        override fun getTom() = tom
    }

    private fun periode(fom: LocalDate?, tom: LocalDate?) = TestPeriode(fom, tom)

    @Test
    fun `løpende periode overlapper året den starter i og alle år etter`() {
        val løpende = periode(LocalDate.of(2023, 6, 1), null)

        løpende.overlapperMedÅr(2022) shouldBe false
        løpende.overlapperMedÅr(2023) shouldBe true
        løpende.overlapperMedÅr(2030) shouldBe true
    }

    @Test
    fun `periode uten startdato er en ødelagt rad og skal feile`() {
        val utenFom = periode(null, LocalDate.of(2023, 6, 1))

        shouldThrow<NullPointerException> {
            utenFom.overlapperMedÅr(2023)
        }.message shouldBe "fom er påkrevd for TestPeriode"
    }

    @Test
    fun `lukket periode overlapper kun årene den dekker`() {
        val lukket = periode(LocalDate.of(2023, 6, 1), LocalDate.of(2024, 2, 1))

        lukket.overlapperMedÅr(2022) shouldBe false
        lukket.overlapperMedÅr(2023) shouldBe true
        lukket.overlapperMedÅr(2024) shouldBe true
        lukket.overlapperMedÅr(2025) shouldBe false
    }
}
