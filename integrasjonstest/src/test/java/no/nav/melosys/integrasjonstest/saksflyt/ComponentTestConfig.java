package no.nav.melosys.integrasjonstest.saksflyt;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.melosys.Application;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.util.SocketUtils;

@EnableMockOAuth2Server
@ComponentScan(basePackageClasses = Application.class)
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
    @Primary
    public Unleash fakeUnleash() {
        return new FakeUnleash();
    }
}
