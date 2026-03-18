package no.nav.melosys.itest

import io.kotest.assertions.json.shouldContainJsonKeyValue
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducerConfig
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Periode
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@ActiveProfiles("test", "local")
@SpringBootTest
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["\${kafka.aiven.a1-utstedt.topic}"],
    brokerProperties = [
        "offsets.topic.replication.factor=1",
        "transaction.state.log.replication.factor=1",
        "transaction.state.log.min.isr=1"
    ]
)
@ContextConfiguration(
    classes = [
        JacksonAutoConfiguration::class,
        KafkaAutoConfiguration::class,
        UtstedtA1AivenProducerConfig::class,
        UtstedtA1AivenProducer::class
    ]
)
class UtstedtA1MeldingSerializationTest(
    @Autowired private val producer: UtstedtA1AivenProducer,
    @Autowired private val kafkaProperties: KafkaProperties,
    @Value("\${kafka.aiven.a1-utstedt.topic}") private val topic: String
) {

    @Test
    @DirtiesContext
    fun `UtstedtA1Melding serialiseres korrekt av Kafka-producer`() {
        val melding = lagMelding()

        producer.produserMelding(melding)

        val json = readFromKafka(topic)
        json.shouldContainJsonKeyValue("$.datoUtstedelse", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.fom", "2025-06-01")
        json.shouldContainJsonKeyValue("$.periode.tom", "2025-09-01")
        json.shouldContainJsonKeyValue("$.artikkel", "11.3a")
        json.shouldContainJsonKeyValue("$.typeUtstedelse", "FØRSTEGANG")
        json.shouldContainJsonKeyValue("$.saksnummer", "MEL-123")
        json.shouldContainJsonKeyValue("$.behandlingId", 123)
    }

    private fun lagMelding() = UtstedtA1Melding(
        "MEL-123",
        123L,
        "1234567898765",
        Lovvalgsbestemmelse.ART_11_3_a,
        Periode(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 9, 1)),
        "SE",
        LocalDate.of(2025, 6, 1),
        A1TypeUtstedelse.FØRSTEGANG
    )

    private fun readFromKafka(topic: String): String {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers.joinToString(","),
            ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-a1-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name
        )
        return KafkaConsumer<String, String>(props).use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = System.currentTimeMillis() + 10_000L
            while (System.currentTimeMillis() < deadline) {
                val record = consumer.poll(Duration.ofMillis(500)).firstOrNull()
                if (record != null) return@use record.value()
            }
            fail("Ingen records mottatt fra topic '$topic' innen 10 sekunder")
        }
    }
}
