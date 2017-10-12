package no.nav.melosys.integrasjon.gsak.behandlesak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;

//FIXME mangler STS config
@Configuration
public class BehandleSakConsumerProducer {
    private BehandleSakConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(BehandleSakConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public BehandleSakConsumer behandleSakConsumer() {
        BehandleSakV1 port = consumerConfig.getPort();
        return new BehandleSakConsumerImpl(port);
    }

    @Bean
    public BehandleSakSelftestConsumer behandleSakSelftestConsumer() {
        BehandleSakV1 port = consumerConfig.getPort();
        return new BehandleSakSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

}
