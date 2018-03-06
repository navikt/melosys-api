package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import no.nav.tjeneste.virksomhet.behandleoppgave.v1.BehandleOppgaveV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandleOppgaveConsumerProducer {

    private BehandleOppgaveConsumerConfig config;

    @Autowired
    public void setConfig(BehandleOppgaveConsumerConfig config) {
        this.config = config;
    }

    @Bean
    public BehandleOppgaveConsumer behandleOppgaveConsumer() {
        BehandleOppgaveV1 port = config.getPort();
        return new BehandleOppgaveConsumerImpl(port);
    }

    @Bean
    public BehandleOppgaveSelftestConsumer behandleOppgaveSelftestConsumer() {
        BehandleOppgaveV1 port = config.getPort();
        return new BehandleOppgaveSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
