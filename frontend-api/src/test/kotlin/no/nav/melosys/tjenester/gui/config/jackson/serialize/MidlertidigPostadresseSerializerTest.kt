package no.nav.melosys.tjenester.gui.config.jackson.serialize

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
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
    fun `skal serialisere midlertidig postadresse Norge med korrekt struktur`() {
        every { kodeverkService.dekod(POSTNUMMER, "0557") } returns "Oslo"

        val midlertidigPostadresse = MidlertidigPostadresseNorge().apply {
            gateadresse = Gateadresse().apply {
                gatenavn = "SANNERGATA"
                husnummer = 2
            }
            poststed = "0557"
            land = Land(NORGE)
        }

        val tree = mapper.readTree(mapper.writeValueAsString(midlertidigPostadresse))

        tree["adressetype"].asText() shouldBe "STRUKTURERT"
        tree["strukturertAdresse"] shouldNotBe null
        tree["strukturertAdresse"]["gatenavn"].asText() shouldBe "SANNERGATA"
        tree["strukturertAdresse"]["husnummer"].asText() shouldBe "2"
        tree["strukturertAdresse"]["postnummer"].asText() shouldBe "0557"
        tree["strukturertAdresse"]["poststed"].asText() shouldBe "Oslo"
        tree["strukturertAdresse"]["landkode"].asText() shouldBe NORGE
        tree.path("ustrukturertAdresse").isMissingNode shouldBe true
    }

    @Test
    fun `skal serialisere midlertidig postadresse utland med korrekt struktur`() {
        val midlertidigPostadresse = MidlertidigPostadresseUtland().apply {
            adresselinje1 = "42 Mock Road"
            adresselinje2 = "Mock City"
            adresselinje3 = "United Kingdom"
            land = Land(STORBRITANNIA)
        }

        val tree = mapper.readTree(mapper.writeValueAsString(midlertidigPostadresse))

        tree["adressetype"].asText() shouldBe "USTRUKTURERT"
        tree["ustrukturertAdresse"] shouldNotBe null
        tree["ustrukturertAdresse"]["landkode"].asText() shouldBe STORBRITANNIA
        tree.path("strukturertAdresse").isMissingNode shouldBe true
    }
}
