package no.nav.melosys.integrasjon.joark.dokumentmottak;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
public class DokumentmottakConsumerConfig extends JmsTemplate {

    private String username;
    private String password;
    private String brokerUrl;

    public DokumentmottakConsumerConfig(@Value("${dokumentmottak.jms.username}") String username,
                                        @Value("${dokumentmottak.jms.password}") String password,
                                        @Value("${dokumentmottak.jms.brokerUrl}") String brokerUrl) {
        this.username = username;
        this.password = password;
        this.brokerUrl = brokerUrl;
    }

    @Bean
    public ConnectionFactory dokumentmottakContainerFactory() {
        return new ActiveMQConnectionFactory(username, password, brokerUrl);
    }
}
