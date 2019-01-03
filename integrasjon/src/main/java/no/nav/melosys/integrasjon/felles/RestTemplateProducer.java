package no.nav.melosys.integrasjon.felles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateProducer {

    private final String euxUri;
    private final String restStsUri;

    @Autowired
    public RestTemplateProducer(@Value("${euxbasis.v1.url}") String euxUri, @Value("${REST_STS.url}") String restStsUri) {
        this.euxUri = euxUri;
        this.restStsUri = restStsUri;
    }

    @Bean
    public RestTemplate euxRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .rootUri(euxUri)
                .build();
    }

    @Bean
    public RestTemplate stsRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .rootUri(restStsUri)
                .build();
    }
}
