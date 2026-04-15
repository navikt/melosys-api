package no.nav.melosys.tjenester.gui.config.jackson

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Tema
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class MelosysModuleTest {

    private lateinit var mapper: JsonMapper
    private val kodeverkService = mockk<KodeverkService>()

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            
            .addModule(KotlinModule.Builder().build())
            .addModule(MelosysModule(kodeverkService))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build()
    }

    @Test
    fun `Kodeverk enum med KodeDto-mapping skal serialiseres som objekt med kode og term`() {
        val json = mapper.writeValueAsString(Behandlingsmaate.MANUELT)
        val tree = mapper.readTree(json)

        tree.has("kode") shouldBe true
        tree.has("term") shouldBe true
        tree.get("kode").asText() shouldBe "MANUELT"
        tree.get("term").asText() shouldBe "Manuelt"
    }

    @Test
    fun `Kodeverk enum uten KodeDto-mapping skal serialiseres som plain string`() {
        val json = mapper.writeValueAsString(Tema.MED)

        json shouldBe "\"MED\""
    }

    @Test
    fun `LocalDate skal serialiseres som ISO-string og ikke som timestamp`() {
        val date = LocalDate.of(2025, 6, 15)

        val json = mapper.writeValueAsString(date)

        json shouldBe "\"2025-06-15\""
        json shouldNotContain "["
    }

    @Test
    fun `data class med Kodeverk-felt skal serialiseres korrekt`() {
        val dto = TestDto(
            behandlingsmaate = Behandlingsmaate.AUTOMATISERT,
            tema = Tema.MED,
            dato = LocalDate.of(2025, 1, 1)
        )

        val json = mapper.writeValueAsString(dto)
        val tree = mapper.readTree(json)

        // behandlingsmaate should be KodeDto
        tree.get("behandlingsmaate").has("kode") shouldBe true
        tree.get("behandlingsmaate").get("kode").asText() shouldBe "AUTOMATISERT"

        // tema should be plain string
        tree.get("tema").isTextual shouldBe true
        tree.get("tema").asText() shouldBe "MED"

        // dato should be ISO string
        tree.get("dato").asText() shouldBe "2025-01-01"
    }

    @Test
    fun `null-verdier i Kodeverk-felt skal haandteres korrekt`() {
        val dto = TestDto(
            behandlingsmaate = null,
            tema = null,
            dato = null
        )

        val json = mapper.writeValueAsString(dto)
        val tree = mapper.readTree(json)

        tree.get("behandlingsmaate").isNull shouldBe true
        tree.get("tema").isNull shouldBe true
        tree.get("dato").isNull shouldBe true
    }

    private data class TestDto(
        val behandlingsmaate: Behandlingsmaate?,
        val tema: Tema?,
        val dato: LocalDate?
    )
}
