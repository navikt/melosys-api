package no.nav.melosys.itest

import tools.jackson.databind.ObjectMapper
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import no.nav.melosys.integrasjon.kafka.ObjectMapperSerializer

@TestConfiguration
class KafkaTestConfig {

    @Bean
    @Qualifier("melosysEessiMelding")
    fun melosysEessiMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper
    ): KafkaTemplate<String, MelosysEessiMelding> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, MelosysEessiMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), ObjectMapperSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Qualifier("jsonSomString")
    fun kafkaTemplateString(
        kafkaProperties: KafkaProperties,
    ): KafkaTemplate<String, String> = KafkaTemplate(
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())
    )

    @Bean
    @Qualifier("manglendeFakturabetalingMelding")
    fun manglendeFakturabetalingMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper
    ): KafkaTemplate<String, ManglendeFakturabetalingMelding> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, ManglendeFakturabetalingMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), ObjectMapperSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Qualifier("skatteHendelseMelding")
    fun skatteHendelseMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper
    ): KafkaTemplate<String, Skattehendelse> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, Skattehendelse> =
            DefaultKafkaProducerFactory(props, StringSerializer(), ObjectMapperSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    @Qualifier("skjemaMottattMelding")
    fun skjemaMottattMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper
    ): KafkaTemplate<String, SkjemaMottattMelding> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, SkjemaMottattMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), ObjectMapperSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }
}
