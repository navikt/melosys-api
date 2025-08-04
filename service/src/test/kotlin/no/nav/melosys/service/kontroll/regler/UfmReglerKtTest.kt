package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test

class UfmReglerKtTest {

    @Test
    fun `statsborgerskap er medlemsland statsborger SE registrer treff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode)) shouldBe true
    }

    @Test
    fun `statsborgerskap er medlemsland statsborger SE og US registrer treff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode, "US")) shouldBe true
    }

    @Test
    fun `statsborgerskap er medlemsland statsborger US ingen treff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf("US")) shouldBe false
    }

    @Test
    fun `lovvalgsland er Norge er Norge registrer treff`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.NO) shouldBe true
    }

    @Test
    fun `lovvalgsland er Norge er Sverige ingen treff`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.SE) shouldBe false
    }
}
