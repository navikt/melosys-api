package no.nav.melosys.tjenester.gui.config.jackson.serialize

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Tema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KodeSerializerTest {

    private lateinit var mapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SimpleModule().apply {
                addSerializer(KodeSerializer())
            })
            .build()
    }

    @Test
    fun `enum som ikke er i IKKE_MAPPES_TIL_KODE_DTO skal serialiseres som KodeDto`() {
        val value = Behandlingsmaate.MANUELT

        val json = mapper.writeValueAsString(value)

        json shouldContain "\"kode\""
        json shouldContain "\"term\""
        json shouldContain "\"MANUELT\""
        json shouldContain "\"Manuelt\""
    }

    @Test
    fun `enum i IKKE_MAPPES_TIL_KODE_DTO skal serialiseres som plain string`() {
        val value = Tema.MED

        val json = mapper.writeValueAsString(value)

        json shouldBe "\"MED\""
    }

    @Test
    fun `enum i IKKE_MAPPES_TIL_KODE_DTO skal ikke inneholde kode-term struktur`() {
        val value = Tema.TRY

        val json = mapper.writeValueAsString(value)

        json shouldNotContain "\"kode\""
        json shouldNotContain "\"term\""
    }

    @Test
    fun `KodeDto-serialisering skal ha riktig JSON-struktur`() {
        val value = Behandlingsmaate.AUTOMATISERT

        val json = mapper.writeValueAsString(value)
        val tree = mapper.readTree(json)

        tree.has("kode") shouldBe true
        tree.has("term") shouldBe true
        tree.get("kode").asText() shouldBe "AUTOMATISERT"
        tree.get("term").asText() shouldBe "Automatisert"
    }
}
