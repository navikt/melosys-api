package no.nav.melosys.service.kafka;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Autowired
    private Environment env;

    @Value("${kafka.aiven.brokers}")
    private String brokersUrl;

    @Value("${kafka.aiven.keystorePath}")
    private String keystorePath;

    @Value("${kafka.aiven.truststorePath}")
    private String truststorePath;

    @Value("${kafka.aiven.credstorePassword}")
    private String credstorePassword;

    @Bean
    @Qualifier("fattetVedtak")
    public KafkaTemplate<String, FattetVedtak> fattetVedtakTemplate(ObjectMapper objectMapper) {
        Map<String, Object> props = commonProps();
        ProducerFactory<String, FattetVedtak> producerFactory =
            new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(objectMapper));

        return new KafkaTemplate<>(producerFactory);
    }

    private Map<String, Object> commonProps() {
        Map<String, Object> props = new HashMap<>();

        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, "melosys-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokersUrl);

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

    private boolean isLocal() {
        return Arrays.stream(env.getActiveProfiles()).anyMatch(profile -> (
            profile.equalsIgnoreCase("local") || profile.equalsIgnoreCase("local-mock")
        ));
    }
}
