package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklartefaktaDtoKonvertererKtTest {

    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer
    private lateinit var avklartefaktaDto: AvklartefaktaDto

    @BeforeEach
    fun setup() {
        avklartefaktaDtoKonverterer = AvklartefaktaDtoKonverterer()
        avklartefaktaDto = AvklartefaktaDto(listOf("Bosted"), "yrkestypevalgliste").apply {
            subjektID = "123456789"
        }
    }

    @Test
    fun testOppdaterAvklartefaktaInnhold() {
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.subjekt shouldBe avklartefaktaDto.subjektID
        avklartefakta.type shouldBe avklartefaktaDto.avklartefaktaType
        avklartefakta.fakta shouldBe avklartefaktaDto.fakta.joinToString(" ")
        avklartefakta.begrunnelseFritekst shouldBe avklartefaktaDto.begrunnelseFritekst
    }

    @Test
    fun testOppdaterAvklartefaktaUtenBegrunnelse() {
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer.shouldBeEmpty()
    }

    @Test
    fun testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.begrunnelseKoder = listOf("Opphold", "Familie")
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer shouldHaveSize 2
        avklartefakta.registreringer shouldNotContain null
    }

    @Test
    fun testOppdaterAvklartefaktaBegrunnelseFritekst() {
        val fritekst = "Fritekst som beskriver begrunnelse"
        avklartefaktaDto.begrunnelseFritekst = fritekst
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer.shouldBeEmpty()
    }
}
