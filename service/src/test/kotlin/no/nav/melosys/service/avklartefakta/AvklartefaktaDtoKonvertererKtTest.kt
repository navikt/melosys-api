package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class AvklartefaktaDtoKonvertererKtTest {

    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer

    private lateinit var avklartefaktaDto: AvklartefaktaDto

    @BeforeEach
    fun setup() {
        avklartefaktaDtoKonverterer = AvklartefaktaDtoKonverterer()
        avklartefaktaDto = AvklartefaktaDto(ArrayList(Collections.singletonList("Bosted")), "yrkestypevalgliste").apply {
            subjektID = "123456789"
        }
    }

    @Test
    fun testOppdaterAvklartefaktaInnhold() {
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.run {
            subjekt shouldBe avklartefaktaDto.subjektID
            type shouldBe avklartefaktaDto.avklartefaktaType
            fakta shouldBe avklartefaktaDto.fakta.joinToString(" ")
            begrunnelseFritekst shouldBe avklartefaktaDto.begrunnelseFritekst
        }
    }

    @Test
    fun testOppdaterAvklartefaktaUtenBegrunnelse() {
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer.shouldBeEmpty()
    }

    @Test
    fun testOppdaterAvklarteFaktaBegrunnelser() {
        avklartefaktaDto.begrunnelseKoder = ArrayList(Arrays.asList("Opphold", "Familie"))
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer.shouldHaveSize(2)
        avklartefakta.registreringer.forEach { registrering ->
            registrering.begrunnelseKode.shouldNotBeNull()
        }
    }

    @Test
    fun testOppdaterAvklartefaktaBegrunnelseFritekst() {
        val fritekst = "Fritekst som beskriver begrunnelse"
        avklartefaktaDto.begrunnelseFritekst = fritekst
        val avklartefakta = avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, null)

        avklartefakta.registreringer.shouldBeEmpty()
    }
}
