package no.nav.melosys.service.kontroll.regler

import com.google.common.collect.Lists
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Landkoder
import org.junit.jupiter.api.Test

class UfmReglerKtTest {

    @Test
    fun statsborgerskapErMedlemsland_statsborgerSE_registrerTreff() {
        UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.kode)) shouldBe true
    }

    @Test
    fun statsborgerskapErMedlemsland_statsborgerSEOgUS_registrerTreff() {
        UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList(Landkoder.SE.kode, "US")) shouldBe true
    }

    @Test
    fun statsborgerskapErMedlemsland_statsborgerUS_ingenTreff() {
        UfmRegler.statsborgerskapErMedlemsland(Lists.newArrayList("US")) shouldBe false
    }

    @Test
    fun lovvalgslandErNorge_erNorge_registrerTreff() {
        UfmRegler.lovvalgslandErNorge(Landkoder.NO) shouldBe true
    }

    @Test
    fun lovvalgslandErNorge_erSverige_ingenTreff() {
        UfmRegler.lovvalgslandErNorge(Landkoder.SE) shouldBe false
    }
}
