package no.nav.melosys.integrasjon.sak;

import no.finn.unleash.Unleash;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SakConsumerProducer {

    private final String endpointUrl;
    private final Unleash unleash;

    public SakConsumerProducer(@Value("${SakAPI_v1.url}") String endpointUrl, Unleash unleash) {
        this.endpointUrl = endpointUrl;
        this.unleash = unleash;
    }

    @Bean
    @Primary
    public SakConsumer sakConsumer() {
        return new SakConsumerImpl(endpointUrl, false, unleash);
    }

    @Bean
    @Qualifier("system")
    public SakConsumer sakSystemConsumer() {
        return new SakConsumerImpl(endpointUrl, true, unleash);
    }

}
