package no.nav.melosys.service.vedtak.publisering;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class FattetVedtakProducerConfig {

    @Bean
    @Qualifier("fattetVedtak")
    public KafkaTemplate<String, FattetVedtak> kafkaTemplate(KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        ProducerFactory<String, FattetVedtak> producerFactory =
            new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(objectMapper));

        return new KafkaTemplate<>(producerFactory);
    }
}
