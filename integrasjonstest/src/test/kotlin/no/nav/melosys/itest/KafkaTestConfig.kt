package no.nav.melosys.itest

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@TestConfiguration
class KafkaTestConfig {

    @Bean
    @Qualifier("melosysEessiMelding")
    fun melosysEessiMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, MelosysEessiMelding> {
        val props = kafkaProperties.buildConsumerProperties(null)
        val producerFactory: ProducerFactory<String, MelosysEessiMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Qualifier("kafkaTemplateString")
    fun kafkaTemplateString(
        kafkaProperties: KafkaProperties,
    ): KafkaTemplate<String, String>  = KafkaTemplate(
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties(null))
    )

    @Bean
    @Qualifier("manglendeFakturabetalingMelding")
    fun ManglendeFakturabetalingMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, ManglendeFakturabetalingMelding> {
        val props = kafkaProperties.buildProducerProperties(null)
        val producerFactory: ProducerFactory<String, ManglendeFakturabetalingMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Qualifier("skatteHendelseMelding")
    fun skatteHendelseMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, Skattehendelse> {
        val props = kafkaProperties.buildProducerProperties(null)
        val producerFactory: ProducerFactory<String, Skattehendelse> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }
}
