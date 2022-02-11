package no.nav.melosys.itest

import com.fasterxml.jackson.databind.ObjectMapper
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.melosysmock.config.GraphqlConfig
import no.nav.melosys.melosysmock.config.SoapConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.util.SocketUtils

@TestConfiguration
@Import(GraphqlConfig::class, SoapConfig::class)
class ComponentTestConfig {
    companion object {
        init {
            System.setProperty("kafkaPort", SocketUtils.findAvailableTcpPort(60000, 65535).toString())
        }
    }

    @Bean
    @Order(1)
    fun kafkaEmbedded(env: Environment): EmbeddedKafkaBroker {
        val kafka = EmbeddedKafkaBroker(
            1, true, 1,
            "teammelosys.eessi.v1-local",
            "teammelosys.soknad-mottak.v1-local",
            "teammelosys.melosys-utstedt-a1.v1-local",
            "teammelosys.fattetvedtak.v1-local"

        )
        kafka.kafkaPorts(env.getRequiredProperty("kafkaPort").toInt())
        kafka.brokerProperty("offsets.topic.replication.factor", 1.toShort())
        kafka.brokerProperty("transaction.state.log.replication.factor", 1.toShort())
        kafka.brokerProperty("transaction.state.log.min.isr", 1)
        return kafka
    }

    @Bean
    @Qualifier("melosysEessiMelding")
    fun melosysEessiMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, MelosysEessiMelding> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, MelosysEessiMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Primary
    fun fakeUnleash(): Unleash {
        return FakeUnleash()
    }
}
