package no.nav.melosys.integrasjon.kodeverk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KodeverkConsumerProducer {

    @Autowired
    private KodeverkConsumerConfig consumerConfig;

    @Bean
    public KodeverkConsumer kodeverkConsumer() {
        return new KodeverkConsumerImpl(consumerConfig.getPort());
    }

    @Bean
    public KodeverkSelftestConsumer kodeverkSelftestConsumer() {
        return new KodeverkSelftestConsumerImpl(consumerConfig.getPort(), consumerConfig.getEndpointUrl());
    }
    
}
