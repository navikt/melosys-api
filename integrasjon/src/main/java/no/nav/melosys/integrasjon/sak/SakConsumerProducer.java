package no.nav.melosys.integrasjon.sak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SakConsumerProducer {

    private final String endpointUrl;

    public SakConsumerProducer(@Value("${SakAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    public SakConsumer sakConsumer() {
        return new SakConsumerImpl(endpointUrl);
    }
}
