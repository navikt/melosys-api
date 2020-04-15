package no.nav.melosys.service.kafka;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.service.soknad.SoknadMottatt;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer2;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MelosysEessiMelding>> eessiMeldingListenerContainerFactory(
        KafkaProperties kafkaProperties, @Value("${kafka.eessi.groupid}") String groupId
    ) {
        return kafkaListenerContainerFactory(MelosysEessiMelding.class, kafkaProperties, groupId);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, SoknadMottatt>> soknadMottattContainerFactory(
        KafkaProperties kafkaProperties, @Value("${kafka.soknad-mottak.groupid}") String groupId
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
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        return props;
    }

    private <T> ErrorHandlingDeserializer2<T> valueDeserializer(Class<T> targetType) {
        return new ErrorHandlingDeserializer2<>(new JsonDeserializer<>(targetType, false));
    }

    @Bean
    public JsonDeserializer<MelosysEessiMelding> jsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<>(MelosysEessiMelding.class, objectMapper, false);
    }
}
