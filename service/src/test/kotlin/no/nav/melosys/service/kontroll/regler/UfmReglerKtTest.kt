package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test

class UfmReglerKtTest {

    @Test
    fun `statsborgerskapErMedlemsland skal returnere true for statsborger SE`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode)) shouldBe true
    }

    @Test
    fun `statsborgerskapErMedlemsland skal returnere true for statsborger SE og US`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode, "US")) shouldBe true
    }

    @Test
    fun `statsborgerskapErMedlemsland skal returnere false for statsborger US`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf("US")) shouldBe false
    }

    @Test
    fun `lovvalgslandErNorge skal returnere true når land er Norge`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.NO) shouldBe true
    }

    @Test
    fun `lovvalgslandErNorge skal returnere false når land er Sverige`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.SE) shouldBe false
    }
}
