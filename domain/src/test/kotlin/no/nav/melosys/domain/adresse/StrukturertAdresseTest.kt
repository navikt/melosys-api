package no.nav.melosys.domain.adresse

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StrukturertAdresseTest {

    @Test
    fun `toList skal returnere formatert liste med variabler`() {
        StrukturertAdresse("Tilleggsnavn", "Haraldsgate", "5C", "CO GG", "2040", "Kløfta", "Viken", "NO").toList()
            .shouldBe(listOf("Tilleggsnavn", "Haraldsgate 5C", "CO GG", "2040", "Kløfta", "Viken", "Norge"))
    }

    @Test
    fun `toList skal returnere formatert liste med variabler uten null eller tommer verdier`() {
        StrukturertAdresse("", "Haraldsgate", "5C", "CO GG", "2040", null, "Viken", "NO").toList()
            .shouldBe(listOf("Haraldsgate 5C", "CO GG", "2040", "Viken", "Norge"))
    }

    @Test
    fun `toList skal kun returnere strengen -Resident outside of Norway- som formatert liste når landkode ikke ligger i Land_iso2`() {
        StrukturertAdresse("", "Singapore gate", "5C", "CO GG", "2040", null, "Singapore region", "SG").toList()
            .shouldBe(listOf("Resident outside of Norway"))
    }

    @Test
    fun `erTom - alle variabler er satt - false`() {
        StrukturertAdresse("Tilleggsnavn", "Haraldsgate", "5C", "CO GG", "2040", "Kløfta", "Viken", "NO").erTom()
            .shouldBe(false)
    }

    @Test
    fun `erTom - alle variabler er satt untatt en - false`() {
        StrukturertAdresse(null, "Haraldsgate", "5C", "CO GG", "2040", "Kløfta", "Viken", "NO").erTom().shouldBe(false)
    }

    @Test
    fun `erTom - ingen variabler er satt - true`() {
        StrukturertAdresse().erTom().shouldBe(true)
    }

    @Test
    fun `erGyldig - norsk adresse - true`() {
        StrukturertAdresse(null, "Haraldsgate", "5C", "CO GG", "2040", "Kløfta", "Viken", "NO").erGyldig()
            .shouldBe(true)
    }

    @Test
    fun `erGyldig - norsk adresse mangler postnummer - false`() {
        StrukturertAdresse(null, "Haraldsgate", "5C", "CO GG", "", "Kløfta", "Viken", "NO").erGyldig()
            .shouldBe(false)
    }

    @Test
    fun `erGyldig - utenlandsk adresse - true`() {
        StrukturertAdresse(null, "Regency street", "55", "CO GG", "1123", "Chelsea", "London", "GB").erGyldig()
            .shouldBe(true)
    }

    @Test
    fun `erGyldig - utenlandsk adresse mangler postnummer - true`() {
        StrukturertAdresse(null, "Regency street", "55", "CO GG", null, "Chelsea", "London", "GB").erGyldig()
            .shouldBe(true)
    }

    @Test
    fun `erGyldig - utenlandsk adresse mangler alt bortsett fra land - false`() {
        StrukturertAdresse(null, "", "", "", null, "", "", "GB").erGyldig()
            .shouldBe(false)
    }
}
