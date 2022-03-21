package no.nav.melosys.integrasjon.kodeverk.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KodeverkConsumerProducer {

    private final String endpointUrl;

    public KodeverkConsumerProducer(@Value("${KodeverkAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    public KodeverkConsumerImpl kodeverkConsumer() {
        return new KodeverkConsumerImpl(endpointUrl);
    }
}
