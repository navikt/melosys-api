package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AvklartMaritimtArbeidKtTest {

    companion object {
        fun lagAvklartefaktaSokkelSkip(navn: String, maritimType: String): Avklartefakta {
            val avklartefakta = Avklartefakta()
            avklartefakta.type = Avklartefaktatyper.SOKKEL_ELLER_SKIP
            avklartefakta.subjekt = navn
            avklartefakta.fakta = maritimType
            return avklartefakta
        }

        fun lagAvklartefaktaArbeidsland(navn: String, landkode: String): Avklartefakta {
            val avklartefakta = Avklartefakta()
            avklartefakta.type = Avklartefaktatyper.ARBEIDSLAND
            avklartefakta.subjekt = navn
            avklartefakta.fakta = landkode
            return avklartefakta
        }
    }

    @Test
    fun `leggTilFakta medTypeSokkel girMaritimTypeSokkel`() {
        val maritimTypeFakta = lagAvklartefaktaSokkelSkip("Stena Don", Maritimtyper.SOKKEL.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", listOf(maritimTypeFakta))

        avklartMaritimtArbeid.maritimtype shouldBe Maritimtyper.SOKKEL
        avklartMaritimtArbeid.land.shouldBeNull()
        avklartMaritimtArbeid.navn shouldBe "Stena Don"
    }

    @Test
    fun `leggTilFakta medTypeArbeidsland girArbeidsland`() {
        val arbeidslandFakta = lagAvklartefaktaArbeidsland("Stena Don", Landkoder.GB.kode)
        val avklartMaritimtArbeid = AvklartMaritimtArbeid("Stena Don", listOf(arbeidslandFakta))

        avklartMaritimtArbeid.land shouldBe Landkoder.GB.kode
        avklartMaritimtArbeid.maritimtype.shouldBeNull()
        avklartMaritimtArbeid.navn shouldBe "Stena Don"
    }
}
