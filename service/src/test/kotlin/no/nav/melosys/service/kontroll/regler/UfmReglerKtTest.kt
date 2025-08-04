package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test

class UfmReglerKtTest {

    @Test
    fun `statsborgerskapErMedlemsland statsborgerSE registrerTreff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode)) shouldBe true
    }

    @Test
    fun `statsborgerskapErMedlemsland statsborgerSEOgUS registrerTreff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf(Landkoder.SE.kode, "US")) shouldBe true
    }

    @Test
    fun `statsborgerskapErMedlemsland statsborgerUS ingenTreff`() {
        UfmRegler.statsborgerskapErMedlemsland(listOf("US")) shouldBe false
    }

    @Test
    fun `lovvalgslandErNorge erNorge registrerTreff`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.NO) shouldBe true
    }

    @Test
    fun `lovvalgslandErNorge erSverige ingenTreff`() {
        UfmRegler.lovvalgslandErNorge(Landkoder.SE) shouldBe false
    }
}
