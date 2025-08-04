package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklartMaritimtArbeidKtTest {

    @Test
    fun leggTilFakta_medTypeSokkel_girMaritimTypeSokkel() {
        val maritimTypeFakta = lagAvklartefaktaSokkelSkip("Stena Don", Maritimtyper.SOKKEL.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", listOf(maritimTypeFakta))

        avklartMaritimtArbeid.maritimtype shouldBe Maritimtyper.SOKKEL
        avklartMaritimtArbeid.land.shouldBeNull()
        avklartMaritimtArbeid.navn shouldBe "Stena Don"
    }

    @Test
    fun leggTilFakta_medTypeArbeidsland_girArbeidsland() {
        val arbeidslandFakta = lagAvklartefaktaArbeidsland("Stena Don", Landkoder.GB.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", listOf(arbeidslandFakta))

        avklartMaritimtArbeid.land shouldBe Landkoder.GB.kode
        avklartMaritimtArbeid.maritimtype.shouldBeNull()
        avklartMaritimtArbeid.navn shouldBe "Stena Don"
    }

    companion object {
        fun lagAvklartefaktaSokkelSkip(navn: String, maritimType: String): Avklartefakta {
            return Avklartefakta().apply {
                type = Avklartefaktatyper.SOKKEL_ELLER_SKIP
                subjekt = navn
                fakta = maritimType
            }
        }

        fun lagAvklartefaktaArbeidsland(navn: String, landkode: String): Avklartefakta {
            return Avklartefakta().apply {
                type = Avklartefaktatyper.ARBEIDSLAND
                subjekt = navn
                fakta = landkode
            }
        }
    }
}
