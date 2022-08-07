package no.nav.melosys.integrasjon.eessi;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EessiConsumerProducer {

    private final String url;

    public EessiConsumerProducer(@Value("${MelosysEessi.url}") String url) {
        this.url = url;
    }

    @Bean
    public EessiConsumer melosysEessiConsumer(ObjectMapper objectMapper,
                                              GenericContextClientRequestInterceptor interceptor,
                                              CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor,
                                              RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.rootUri(url).build();
        restTemplate.setInterceptors(Arrays.asList(interceptor, correlationIdOutgoingInterceptor));
        return new EessiConsumerImpl(restTemplate, objectMapper);
    }
}
