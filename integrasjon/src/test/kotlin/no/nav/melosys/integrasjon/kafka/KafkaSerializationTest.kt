package no.nav.melosys.integrasjon.kafka

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import tools.jackson.module.kotlin.kotlinModule
import java.time.LocalDate
import java.util.UUID

class KafkaSerializationTest {

    private val objectMapper = JsonMapper.builder().build()

    @Nested
    inner class ObjectMapperSerializerTest {

        private val serializer = ObjectMapperSerializer<TestMessage>(objectMapper)

        @Test
        fun `serialize returns JSON bytes for valid object`() {
            val message = TestMessage("hello", 42)

            val bytes = serializer.serialize("test-topic", message)

            bytes shouldNotBe null
            val json = String(bytes!!, Charsets.UTF_8)
            json shouldContain "\"name\":\"hello\""
            json shouldContain "\"value\":42"
        }

        @Test
        fun `serialize returns null for null data`() {
            val bytes = serializer.serialize("test-topic", null)

            bytes shouldBe null
        }

        @Test
        fun `serialize handles object with LocalDate`() {
            val dateSerializer = ObjectMapperSerializer<TestMessageWithDate>(objectMapper)
            val message = TestMessageWithDate("test", LocalDate.of(2025, 1, 15))

            val bytes = dateSerializer.serialize("test-topic", message)

            val json = String(bytes!!, Charsets.UTF_8)
            json shouldContain "2025-01-15"
        }

        @Test
        fun `roundtrip serialize and deserialize produces equivalent object`() {
            val original = TestMessage("roundtrip", 99)

            val bytes = serializer.serialize("test-topic", original)
            val deserialized = objectMapper.readValue(bytes, TestMessage::class.java)

            deserialized.name shouldBe original.name
            deserialized.value shouldBe original.value
        }
    }

    @Nested
    inner class LoggingDeserializerTest {

        private val deserializer = LoggingDeserializer(objectMapper, TestMessage::class.java)

        @Test
        fun `deserialize returns object for valid JSON`() {
            val json = """{"name":"hello","value":42}"""

            val result = deserializer.deserialize("test-topic", json.toByteArray())

            result.name shouldBe "hello"
            result.value shouldBe 42
        }

        @Test
        fun `deserialize throws FailedDeserializationException for null data`() {
            val exception = shouldThrow<FailedDeserializationException> {
                deserializer.deserialize("test-topic", null)
            }

            exception.topic shouldBe "test-topic"
            exception.rawMessage shouldBe "null data"
        }

        @Test
        fun `deserialize throws FailedDeserializationException for invalid JSON`() {
            val exception = shouldThrow<FailedDeserializationException> {
                deserializer.deserialize("test-topic", "not json".toByteArray())
            }

            exception.topic shouldBe "test-topic"
            exception.rawMessage shouldBe "not json"
        }

        @Test
        fun `deserialize uses 'unknown' as topic when topic is null`() {
            val exception = shouldThrow<FailedDeserializationException> {
                deserializer.deserialize(null, null)
            }

            exception.topic shouldBe "unknown"
        }

        @Test
        fun `FailedDeserializationException preserves cause`() {
            val exception = shouldThrow<FailedDeserializationException> {
                deserializer.deserialize("my-topic", "bad".toByteArray())
            }

            exception.cause shouldNotBe null
            exception.message shouldContain "my-topic"
            exception.message shouldContain "bad"
        }
    }

    /**
     * Kompatibilitet for SkjemaMottattMelding ved utrulling av gruppeId (MELOSYS-8151).
     *
     * De to appene rulles ikke ut samtidig, så meldinger må kunne leses både med og uten det nye
     * feltet. Mapperen speiler produksjonsoppsettet i application.yml
     * (spring.jackson.deserialization.fail-on-unknown-properties: false).
     */
    @Nested
    inner class SkjemaMottattMeldingKompatibilitetTest {

        private val produksjonslikMapper: ObjectMapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        private val deserializer =
            LoggingDeserializer(produksjonslikMapper, SkjemaMottattMelding::class.java)

        @Test
        fun `melding uten gruppeId leses med gruppeId null - gammel produsent, ny konsument`() {
            val skjemaId = UUID.randomUUID()
            val json = """{"skjemaId":"$skjemaId","relaterteSkjemaIder":[]}"""

            val melding = deserializer.deserialize("topic", json.toByteArray())

            melding.skjemaId shouldBe skjemaId
            melding.gruppeId shouldBe null
        }

        @Test
        fun `melding med gruppeId leses - ny produsent, ny konsument`() {
            val skjemaId = UUID.randomUUID()
            val gruppeId = UUID.randomUUID()
            val json = """{"skjemaId":"$skjemaId","relaterteSkjemaIder":[],"gruppeId":"$gruppeId"}"""

            deserializer.deserialize("topic", json.toByteArray()).gruppeId shouldBe gruppeId
        }

        @Test
        fun `ukjent felt kaster ikke - ny produsent, gammel konsument`() {
            // Sikrer at rekkefølgen på utrulling ikke kan velte konsumenten. Skulle noen slå på
            // FAIL_ON_UNKNOWN_PROPERTIES, feiler denne testen i stedet for produksjon.
            val skjemaId = UUID.randomUUID()
            val json = """{"skjemaId":"$skjemaId","relaterteSkjemaIder":[],"etHeltNyttFelt":"verdi"}"""

            deserializer.deserialize("topic", json.toByteArray()).skjemaId shouldBe skjemaId
        }
    }

    data class TestMessage(val name: String = "", val value: Int = 0)
    data class TestMessageWithDate(val name: String = "", val date: LocalDate? = null)
}
