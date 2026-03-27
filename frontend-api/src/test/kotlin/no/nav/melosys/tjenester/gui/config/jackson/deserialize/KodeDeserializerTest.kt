package no.nav.melosys.tjenester.gui.config.jackson.deserialize

import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationConfig
import tools.jackson.databind.JavaType
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleDeserializers
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.tjenester.gui.config.jackson.serialize.KodeSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KodeDeserializerTest {

    private lateinit var mapper: tools.jackson.databind.ObjectMapper

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SimpleModule().apply {
                addSerializer(KodeSerializer())
                setDeserializers(object : SimpleDeserializers() {
                    override fun findEnumDeserializer(
                        enumType: JavaType,
                        config: DeserializationConfig,
                        beanDescRef: BeanDescription.Supplier
                    ): ValueDeserializer<*>? {
                        if (Kodeverk::class.java.isAssignableFrom(enumType.rawClass)) {
                            @Suppress("UNCHECKED_CAST")
                            return KodeDeserializer(enumType.rawClass as Class<out Kodeverk>)
                        }
                        return null
                    }
                })
            })
            .build()
    }

    @Test
    fun `skal deserialisere fra plain string`() {
        val result = mapper.readValue("\"MANUELT\"", Behandlingsmaate::class.java)

        result shouldBe Behandlingsmaate.MANUELT
    }

    @Test
    fun `skal deserialisere fra KodeDto-objekt`() {
        val json = """{"kode":"MANUELT","term":"Manuelt"}"""

        val result = mapper.readValue(json, Behandlingsmaate::class.java)

        result shouldBe Behandlingsmaate.MANUELT
    }

    @Test
    fun `skal deserialisere enum i IKKE_MAPPES_TIL_KODE_DTO fra string`() {
        val result = mapper.readValue("\"MED\"", Tema::class.java)

        result shouldBe Tema.MED
    }

    @Test
    fun `roundtrip - serialisering og deserialisering av KodeDto-type`() {
        val original = Behandlingsmaate.AUTOMATISERT

        val json = mapper.writeValueAsString(original)
        val result = mapper.readValue(json, Behandlingsmaate::class.java)

        result shouldBe original
    }

    @Test
    fun `roundtrip - serialisering og deserialisering av string-type`() {
        val original = Tema.TRY

        val json = mapper.writeValueAsString(original)
        val result = mapper.readValue(json, Tema::class.java)

        result shouldBe original
    }

    @Test
    fun `skal deserialisere null`() {
        val result = mapper.readValue("null", Behandlingsmaate::class.java)

        result shouldBe null
    }

    @Test
    fun `roundtrip - DTO med Kodeverk-felt`() {
        val original = TestDto(Behandlingsmaate.MANUELT, Tema.MED)

        val json = mapper.writeValueAsString(original)
        val result = mapper.readValue(json, TestDto::class.java)

        result.behandlingsmaate shouldBe original.behandlingsmaate
        result.tema shouldBe original.tema
    }

    data class TestDto(
        val behandlingsmaate: Behandlingsmaate? = null,
        val tema: Tema? = null
    )
}
