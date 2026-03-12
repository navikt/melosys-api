package no.nav.melosys.tjenester.gui.config.jackson.serialize

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk.POSTNUMMER
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Land.Companion.NORGE
import no.nav.melosys.domain.dokument.felles.Land.Companion.STORBRITANNIA
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MidlertidigPostadresseSerializerTest {

    private lateinit var mapper: ObjectMapper
    private val kodeverkService = mockk<KodeverkService>()

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL) }
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .addModule(SimpleModule().apply {
                addSerializer(MidlertidigPostadresseSerializer(kodeverkService))
            })
            .build()
    }

    @Test
    fun `skal serialisere midlertidig postadresse Norge`() {
        every { kodeverkService.dekod(POSTNUMMER, "0557") } returns "Oslo"

        val midlertidigPostadresse = MidlertidigPostadresseNorge().apply {
            gateadresse = Gateadresse().apply {
                gatenavn = "SANNERGATA"
                husnummer = 2
            }
            poststed = "0557"
            land = Land(NORGE)
        }


        val json = mapper.writeValueAsString(midlertidigPostadresse)


        json shouldNotBe null
    }

    @Test
    fun `skal serialisere midlertidig postadresse utland`() {
        val midlertidigPostadresse = MidlertidigPostadresseUtland().apply {
            adresselinje1 = "42 Mock Road"
            adresselinje2 = "Mock City"
            adresselinje3 = "United Kingdom"
            land = Land(STORBRITANNIA)
        }


        val json = mapper.writeValueAsString(midlertidigPostadresse)


        json shouldNotBe null
    }
}
