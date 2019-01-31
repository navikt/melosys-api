package no.nav.melosys.integrasjon.eux.consumer;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EuxConsumerProducer {

    private final RestTemplate euxRestTemplate;
    private final RestStsClient restStsClient;

    @Autowired
    public EuxConsumerProducer(@Qualifier("euxRestTemplate") RestTemplate euxRestTemplate, RestStsClient restStsClient) {
        this.euxRestTemplate = euxRestTemplate;
        this.restStsClient = restStsClient;
    }

    @Bean
    @Primary
    public EuxConsumer euxConsumer() {
        return new EuxConsumerImpl(euxRestTemplate, restStsClient, false);
    }

    @Bean(name = "system")
    public EuxConsumer euxConsumerSystem() {
        return new EuxConsumerImpl(euxRestTemplate, restStsClient, true);
    }
}
