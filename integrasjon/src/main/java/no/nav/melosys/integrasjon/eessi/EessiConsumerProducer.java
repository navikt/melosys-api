package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor;
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
                                              RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return new EessiConsumerImpl(restTemplate, objectMapper);
    }
}
