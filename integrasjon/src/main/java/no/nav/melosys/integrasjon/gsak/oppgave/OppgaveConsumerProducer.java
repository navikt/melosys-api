package no.nav.melosys.integrasjon.gsak.oppgave;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OppgaveConsumerProducer {

    private final OppgaveConsumerConfig config;

    @Autowired
    public OppgaveConsumerProducer(OppgaveConsumerConfig config) {
        this.config = config;
    }

    @Bean
    public OppgaveConsumer oppgaveApiConsumer() {
        return new OppgaveConsumerImpl(config.getEndpointUrl());
    }
}
