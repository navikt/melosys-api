package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import org.junit.jupiter.api.Test
import java.util.*

class AvklartMaritimtArbeidTest {

    @Test
    fun `leggTilFakta med type sokkel gir maritimtype sokkel`() {
        val maritimTypeFakta = lagAvklartefaktaSokkelSkip("Stena Don", Maritimtyper.SOKKEL.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", Collections.singletonList(maritimTypeFakta))

        avklartMaritimtArbeid.run {
            maritimtype shouldBe Maritimtyper.SOKKEL
            land.shouldBeNull()
            navn shouldBe "Stena Don"
        }
    }

    @Test
    fun `leggTilFakta med type arbeidsland gir arbeidsland`() {
        val arbeidslandFakta = lagAvklartefaktaArbeidsland("Stena Don", Landkoder.GB.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", Collections.singletonList(arbeidslandFakta))

        avklartMaritimtArbeid.run {
            land shouldBe Landkoder.GB.kode
            maritimtype.shouldBeNull()
            navn shouldBe "Stena Don"
        }
    }
    
    companion object {
        fun lagAvklartefaktaSokkelSkip(navn: String, maritimType: String) = Avklartefakta().apply {
            type = Avklartefaktatyper.SOKKEL_ELLER_SKIP
            subjekt = navn
            fakta = maritimType
        }

        fun lagAvklartefaktaArbeidsland(navn: String, landkode: String) = Avklartefakta().apply {
            type = Avklartefaktatyper.ARBEIDSLAND
            subjekt = navn
            fakta = landkode
        }
    }
}
