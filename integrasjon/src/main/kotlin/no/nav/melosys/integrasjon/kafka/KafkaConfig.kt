package no.nav.melosys.integrasjon.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.integrasjon.SoknadMottatt
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

typealias KafkaConsumerContainerFactory<T> = KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, T>>

@Configuration
@EnableKafka
class KafkaConfig(
    private val env: Environment,
    @Value("\${kafka.aiven.brokers}") private val brokersUrl: String,
    @Value("\${kafka.aiven.keystorePath}") private val keystorePath: String,
    @Value("\${kafka.aiven.truststorePath}") private val truststorePath: String,
    @Value("\${kafka.aiven.credstorePassword}") private val credstorePassword: String
) {

    @Bean
    fun aivenEessiMeldingListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        @Value("\${kafka.aiven.eessi.groupid}") groupId: String
    ): KafkaConsumerContainerFactory<MelosysEessiMelding> = kafkaListenerContainerFactory<MelosysEessiMelding>(kafkaProperties, groupId)

    @Bean
    fun aivenManglendeFakturabetalingMeldingListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        @Value("\${kafka.aiven.manglende-fakturabetaling.groupid}") groupId: String
    ): KafkaConsumerContainerFactory<ManglendeFakturabetalingMelding> =
        kafkaListenerContainerFactory<ManglendeFakturabetalingMelding>(kafkaProperties, groupId)

    @Bean
    fun aivenSoknadMottattContainerFactory(
        kafkaProperties: KafkaProperties,
        @Value("\${kafka.aiven.soknad-mottak.groupid}") groupId: String
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, SoknadMottatt>> =
        kafkaListenerContainerFactory<SoknadMottatt>(kafkaProperties, groupId)

    @Bean
    fun producerFactoryMelosysHendelse(objectMapper: ObjectMapper): ProducerFactory<String, MelosysHendelse> =
        DefaultKafkaProducerFactory(
            mutableMapOf<String, Any>(
                CommonClientConfigs.CLIENT_ID_CONFIG to "melosys-producer",
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokersUrl,
            ) + securityConfig(),
            StringSerializer(),
            JsonSerializer(objectMapper)
        )

    @Bean
    @Qualifier("melosysHendelse")
    fun melosysHendelseKafkaTemplate(producerFactory: ProducerFactory<String, MelosysHendelse>): KafkaTemplate<String, MelosysHendelse> =
        KafkaTemplate(producerFactory)


    private inline fun <reified T> kafkaListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        groupId: String
    ) = ConcurrentKafkaListenerContainerFactory<String, T>().apply {
        consumerFactory = DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(null) + (mapOf<String, Any>(
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokersUrl,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 15000,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1
            ) + securityConfig()),
            StringDeserializer(),
            valueDeserializer<T>()
        )
    }

    private inline fun <reified T> valueDeserializer(): ErrorHandlingDeserializer<T> =
        ErrorHandlingDeserializer(
            JsonDeserializer(T::class.java, false)
        )

    private fun securityConfig(): Map<String, Any> =
        if (isLocal) mapOf() else mapOf<String, Any>(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to truststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12"
        )

    private val isLocal: Boolean
        get() = env.activeProfiles.any {
            it.equals("local", ignoreCase = true) ||
                it.equals("local-mock", ignoreCase = true) ||
                it.equals("test", ignoreCase = true)
        }
}
