package no.nav.melosys.itest

import io.kotest.assertions.json.shouldEqualJson
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler
import no.nav.melosys.integrasjon.popp.KafkaPensjonsopptjeningHendelseProducer
import no.nav.melosys.integrasjon.popp.PensjonsopptjeningHendelse
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
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["\${kafka.aiven.popp-hendelser.topic}"],
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
        KafkaConfig::class,
        SkippableKafkaErrorHandler::class,
        KafkaPensjonsopptjeningHendelseProducer::class
    ]
)
class PensjonsopptjeningHendelseKafkaSerializationTest(
    @Autowired private val producer: KafkaPensjonsopptjeningHendelseProducer,
    @Autowired private val kafkaProperties: KafkaProperties,
    @Value("\${kafka.aiven.popp-hendelser.topic}") private val topic: String
) {

    @Test
    @DirtiesContext
    fun `PensjonsopptjeningHendelse serialiseres korrekt av Kafka-producer`() {
        val hendelse = PensjonsopptjeningHendelse(
            hendelsesId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            fnr = "12345678901",
            pgi = 500_000L,
            inntektsAr = 2023,
            fastsattTidspunkt = LocalDateTime.of(2024, 3, 15, 10, 30, 0),
            endringstype = PensjonsopptjeningHendelse.Endringstype.NY_INNTEKT,
            melosysBehandlingID = 42L
        )

        producer.sendPensjonsopptjeningHendelse(hendelse)

        val json = readFromKafka(topic)
        json shouldEqualJson """
            {
                "hendelsesId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "fnr": "12345678901",
                "pgi": 500000,
                "inntektsAr": 2023,
                "fastsattTidspunkt": "2024-03-15T10:30:00",
                "endringstype": "NY_INNTEKT",
                "melosysBehandlingID": 42
            }
        """
    }

    private fun readFromKafka(topic: String): String {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers.joinToString(","),
            ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-popp-${UUID.randomUUID()}",
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
