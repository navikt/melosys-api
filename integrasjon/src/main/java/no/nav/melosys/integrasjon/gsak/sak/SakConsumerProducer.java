package no.nav.melosys.integrasjon.gsak.sak;

import no.nav.melosys.exception.IntegrasjonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SakConsumerProducer {

    private final String endpointUrl;

    @Autowired
    public SakConsumerProducer(@Value("${SakAPI_v1.url}") String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Bean
    @Primary
    public SakConsumer sakConsumer() throws IntegrasjonException {
        return new SakConsumerImpl(endpointUrl, false);
    }

    @Bean
    @Qualifier("system")
    public SakConsumer sakSystemConsumer() throws IntegrasjonException {
        return new SakConsumerImpl(endpointUrl, true);
    }

}
