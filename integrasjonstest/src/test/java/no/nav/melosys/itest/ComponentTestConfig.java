package no.nav.melosys.itest;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.melosysmock.config.GraphqlConfig;
import no.nav.melosys.melosysmock.config.SoapConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.util.SocketUtils;

@TestConfiguration
@Import(
    {
        GraphqlConfig.class,
        SoapConfig.class,
    }
)
public class ComponentTestConfig {

    static {
        System.setProperty("kafkaPort", String.valueOf(SocketUtils.findAvailableTcpPort(60000, 65535)));
    }

    @Bean
    @Order(1)
    EmbeddedKafkaBroker kafkaEmbedded(Environment env) {
        EmbeddedKafkaBroker kafka = new EmbeddedKafkaBroker(1, true, 1,
            "privat-melosys-eessi-v1-local",
            "privat-melosys-soknad-mottak-local",
            "aapen-melosys-utstedtA1-v1-local",
            "teammelosys.fattetvedtak.v1-local");
        kafka.kafkaPorts(Integer.parseInt(env.getRequiredProperty("kafkaPort")));
        kafka.brokerProperty("offsets.topic.replication.factor", (short) 1);
        kafka.brokerProperty("transaction.state.log.replication.factor", (short) 1);
        kafka.brokerProperty("transaction.state.log.min.isr", 1);

        return kafka;
    }

    @Bean
    @Qualifier("melosysEessiMelding")
    public KafkaTemplate<String, MelosysEessiMelding> melosysEessiMeldingKafkaTemplate(KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        ProducerFactory<String, MelosysEessiMelding> producerFactory =
            new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(objectMapper));

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @Primary
    public Unleash fakeUnleash() {
        return new FakeUnleash();
    }

}
