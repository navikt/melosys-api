package no.nav.melosys.itest

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.kafka.KafkaConsumerContainerFactory
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@TestConfiguration
class KafkaTestConfig {

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
    @Qualifier("manglendeFakturabetalingMelding")
    fun ManglendeFakturabetalingMeldingKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, ManglendeFakturabetalingMelding> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, ManglendeFakturabetalingMelding> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }

    @Bean
    fun melosysMeldingListenerContainerFactory(
        kafkaProperties: KafkaProperties
    ): KafkaConsumerContainerFactory<MelosysHendelse> =
        ConcurrentKafkaListenerContainerFactory<String, MelosysHendelse>().apply {
            consumerFactory = DefaultKafkaConsumerFactory(
                kafkaProperties.buildConsumerProperties(null),
                StringDeserializer(),
                JsonDeserializer(MelosysHendelse::class.java, false)
            )
        }
}
