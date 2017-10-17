package no.nav.melosys.integrasjon.joark.journal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2;

//FIXME mangler STS config
@Configuration
public class JournalConsumerProducer {
    private JournalConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(JournalConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public JournalConsumer journalConsumer() {
        JournalV2 port = consumerConfig.getPort();
        return new JournalConsumerImpl(port);
    }

    @Bean
    public JournalSelftestConsumer journalSelftestConsumer() {
        JournalV2 port = consumerConfig.getPort();
        return new JournalSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

}
