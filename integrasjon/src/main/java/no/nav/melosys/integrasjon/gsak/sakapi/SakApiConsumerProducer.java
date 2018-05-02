package no.nav.melosys.integrasjon.gsak.sakapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SakApiConsumerProducer {

    private SakApiConsumerConfig config;

    @Autowired
    public SakApiConsumerProducer(SakApiConsumerConfig config) {
        this.config = config;
    }

    @Bean
    public SakApiConsumer sakApiConsumer() {
        return new SakApiConsumerImpl(config.getEndpointUrl());
    }

}
