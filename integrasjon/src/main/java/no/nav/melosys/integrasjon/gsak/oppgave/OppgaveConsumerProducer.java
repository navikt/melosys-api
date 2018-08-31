package no.nav.melosys.integrasjon.gsak.oppgave;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OppgaveConsumerProducer {

    private final String endpointUrl;

    @Autowired
    public OppgaveConsumerProducer(@Value("${OppgaveAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    @Primary
    public OppgaveConsumer oppgaveConsumer() {
        return new OppgaveConsumerImpl(endpointUrl, false);
    }

    @Bean
    @Qualifier("system")
    public OppgaveConsumer oppgaveSystemConsumer() {
        return new OppgaveConsumerImpl(endpointUrl, true);
    }
}
