package no.nav.melosys.itest

import io.kotest.assertions.json.shouldEqualJson
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler
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

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["\${kafka.aiven.melosys-hendelser.topic}"],
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
        KafkaMelosysHendelseProducer::class
    ]
)
class VedtakHendelseMeldingKafkaSerializationTest(
    @Autowired private val producer: KafkaMelosysHendelseProducer,
    @Autowired private val kafkaProperties: KafkaProperties,
    @Value("\${kafka.aiven.melosys-hendelser.topic}") private val topic: String
) {

    @Test
    @DirtiesContext
    fun `VedtakHendelseMelding serialiseres korrekt av Kafka-producer`() {
        val hendelse = MelosysHendelse(
            VedtakHendelseMelding(
                folkeregisterIdent = "12345678901",
                sakstype = Sakstyper.TRYGDEAVTALE,
                sakstema = Sakstemaer.TRYGDEAVGIFT,
                behandligsresultatType = Behandlingsresultattyper.FERDIGBEHANDLET,
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK,
                medlemskapsperioder = listOf(
                    Periode(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2022, 12, 31),
                        innvilgelsesResultat = InnvilgelsesResultat.INNVILGET
                    )
                ),
                lovvalgsperioder = listOf()
            )
        )

        producer.produserBestillingsmelding(hendelse)

        val json = readFromKafka(topic)
        json shouldEqualJson """
            {
                "melding": {
                    "type": "VedtakHendelseMelding",
                    "folkeregisterIdent": "12345678901",
                    "sakstype": "TRYGDEAVTALE",
                    "sakstema": "TRYGDEAVGIFT",
                    "behandligsresultatType": "FERDIGBEHANDLET",
                    "vedtakstype": "FØRSTEGANGSVEDTAK",
                    "medlemskapsperioder": [
                        {
                            "fom": "2021-01-01",
                            "tom": "2022-12-31",
                            "innvilgelsesResultat": "INNVILGET"
                        }
                    ],
                    "lovvalgsperioder": []
                }
            }
        """
    }

    private fun readFromKafka(topic: String): String {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers.joinToString(","),
            ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-vedtak-${UUID.randomUUID()}",
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
