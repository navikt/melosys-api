package no.nav.melosys.service.kafka;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.service.vedtak.publisering.dto.FattetVedtak;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${kafka.aiven.brokers}")
    private String brokersUrl;

    @Value("${kafka.aiven.certificate}")
    private String certificate;

    @Value("${kafka.aiven.certificatePath}")
    private String certificatePath;

    @Value("${kafka.aiven.privateKey}")
    private String privateKey;

    @Value("${kafka.aiven.privateKeyPath}")
    private String privateKeyPath;

    @Value("${kafka.aiven.ca}")
    private String ca;

    @Value("${kafka.aiven.caPath}")
    private String caPath;

    @Value("${kafka.aiven.keystorePath}")
    private String keystorePath;

    @Value("${kafka.aiven.truststorePath}")
    private String truststorePath;

    @Value("${kafka.aiven.credstorePassword}")
    private String credstorePassword;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Bean
    @Qualifier("fattetVedtak")
    public KafkaTemplate<String, FattetVedtak> fattetVedtakTemplate() {
        Map<String, Object> props = commonProps();
        ProducerFactory<String, FattetVedtak> producerFactory =
            new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(OBJECT_MAPPER));

        return new KafkaTemplate<>(producerFactory);
    }

    private Map<String, Object> commonProps() {
        Map<String, Object> props = new HashMap<>();

        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, "melosys-api-producer");
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credstorePassword);
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credstorePassword);
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, credstorePassword);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");

        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokersUrl);
        return props;
    }
}
