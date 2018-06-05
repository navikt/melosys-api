package no.nav.melosys.integrasjon.sakogbehandling;

import javax.jms.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;

@Profile("!mocking") //FIXME MELOSYS-1034
@Configuration
public class SakOgBehandlingClientConfig {

    // FIXME: JMS-integrasjon implementeres av MELOSYS-1034
    private JmsTemplate jmsTemplate;

    private Queue hendelseshåndterer;

    @Bean
    public SakOgBehandlingClient sakOgBehandlingClient() {
        return new SakOgBehandlingClientImpl(jmsTemplate, hendelseshåndterer);
    }
}
