package no.nav.melosys.service.kafka;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
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

    private final String kafkaGroupId;

    public KafkaConfig(@Value("${kafka.groupid}") String kafkaGroupId) {
        this.kafkaGroupId = kafkaGroupId;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MelosysEessiMelding>> eessiMeldingListenerContainerFactory(
        KafkaProperties kafkaProperties, ErrorHandlingDeserializer2<MelosysEessiMelding> errorHandlingDeserializer) {

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.putAll(consumerConfig());
        DefaultKafkaConsumerFactory<String, MelosysEessiMelding> defaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), errorHandlingDeserializer);
        ConcurrentKafkaListenerContainerFactory<String, MelosysEessiMelding> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultKafkaConsumerFactory);

        return factory;
    }

    private Map<String, Object> consumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        return props;
    }
    
    @Bean
    public ErrorHandlingDeserializer2<MelosysEessiMelding> valueDeserializer(JsonDeserializer<MelosysEessiMelding> jsonDeserializer) {
        return new ErrorHandlingDeserializer2<>(jsonDeserializer);
    }

    @Bean
    public JsonDeserializer<MelosysEessiMelding> jsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<>(MelosysEessiMelding.class, objectMapper, false);
    }
}
