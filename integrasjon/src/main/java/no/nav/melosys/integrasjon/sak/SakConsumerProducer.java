package no.nav.melosys.integrasjon.sak;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SakConsumerProducer {

    private final String endpointUrl;

    public SakConsumerProducer(@Value("${SakAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    @Primary
    public SakConsumer sakConsumer() {
        return new SakConsumerImpl(endpointUrl, false);
    }

    @Bean
    @Qualifier("system")
    public SakConsumer sakSystemConsumer() {
        return new SakConsumerImpl(endpointUrl, true);
    }

}
