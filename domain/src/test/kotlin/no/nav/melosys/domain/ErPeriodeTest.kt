package no.nav.melosys.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ErPeriodeTest {

    private fun periode(fom: LocalDate?, tom: LocalDate?) = object : ErPeriode {
        override fun getFom() = fom
        override fun getTom() = tom
    }

    @Test
    fun `løpende periode overlapper året den starter i og alle år etter`() {
        val løpende = periode(LocalDate.of(2023, 6, 1), null)

        løpende.overlapperMedÅr(2022) shouldBe false
        løpende.overlapperMedÅr(2023) shouldBe true
        løpende.overlapperMedÅr(2030) shouldBe true
    }

    @Test
    fun `periode uten startdato overlapper året den slutter i og alle år før`() {
        val utenFom = periode(null, LocalDate.of(2023, 6, 1))

        utenFom.overlapperMedÅr(2022) shouldBe true
        utenFom.overlapperMedÅr(2023) shouldBe true
        utenFom.overlapperMedÅr(2024) shouldBe false
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
