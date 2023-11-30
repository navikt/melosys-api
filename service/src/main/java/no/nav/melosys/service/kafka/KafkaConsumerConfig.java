package no.nav.melosys.service.kafka;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding;
import no.nav.melosys.service.soknad.SoknadMottatt;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private final Environment env;
    private final String brokersUrl;
    private final String keystorePath;
    private final String truststorePath;
    private final String credstorePassword;

    public KafkaConsumerConfig(Environment env,
                               @Value("${kafka.aiven.brokers}") String brokersUrl,
                               @Value("${kafka.aiven.keystorePath}") String keystorePath,
                               @Value("${kafka.aiven.truststorePath}") String truststorePath,
                               @Value("${kafka.aiven.credstorePassword}") String credstorePassword) {
        this.env = env;
        this.brokersUrl = brokersUrl;
        this.keystorePath = keystorePath;
        this.truststorePath = truststorePath;
        this.credstorePassword = credstorePassword;
    }

    @Bean
    public JsonDeserializer<MelosysEessiMelding> jsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<>(MelosysEessiMelding.class, objectMapper, false);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MelosysEessiMelding>> aivenEessiMeldingListenerContainerFactory(
        KafkaProperties kafkaProperties, @Value("${kafka.aiven.eessi.groupid}") String groupId
    ) {
        return kafkaListenerContainerFactory(MelosysEessiMelding.class, kafkaProperties, groupId);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ManglendeFakturabetalingMelding>> aivenManglendeFakturabetalingMeldingListenerContainerFactory(
        KafkaProperties kafkaProperties, @Value("${kafka.aiven.manglende-fakturabetaling.groupid}") String groupId
    ) {
        return kafkaListenerContainerFactory(ManglendeFakturabetalingMelding.class, kafkaProperties, groupId);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, SoknadMottatt>> aivenSoknadMottattContainerFactory(
        KafkaProperties kafkaProperties, @Value("${kafka.aiven.soknad-mottak.groupid}") String groupId
    ) {
        return kafkaListenerContainerFactory(SoknadMottatt.class, kafkaProperties, groupId);
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory(
        Class<T> containerType, KafkaProperties kafkaProperties, String groupId) {

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.putAll(consumerConfig(groupId));
        DefaultKafkaConsumerFactory<String, T> defaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), valueDeserializer(containerType));
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultKafkaConsumerFactory);

        return factory;
    }

    private Map<String, Object> consumerConfig(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokersUrl);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        if (!isLocal()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credstorePassword);
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");

            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credstorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, credstorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        }

        return props;
    }

    private <T> ErrorHandlingDeserializer<T> valueDeserializer(Class<T> targetType) {
        return new ErrorHandlingDeserializer<>(new JsonDeserializer<>(targetType, false));
    }

    private boolean isLocal() {
        return Arrays.stream(env.getActiveProfiles()).anyMatch(profile -> (
            profile.equalsIgnoreCase("local") || profile.equalsIgnoreCase("local-mock") || profile.equalsIgnoreCase("test")
        ));
    }

}
