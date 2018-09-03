package no.nav.melosys.integrasjon.gsak.sak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SakApiConsumerProducer {

    private final String endpointUrl;

    @Autowired
    public SakApiConsumerProducer(@Value("${SakAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    @Primary
    public SakApiConsumer sakConsumer() {
        return new SakApiConsumerImpl(endpointUrl, false);
    }

    @Bean
    @Qualifier("system")
    public SakApiConsumer sakSystemConsumer() {
        return new SakApiConsumerImpl(endpointUrl, true);
    }

}
