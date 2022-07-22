package no.nav.melosys.integrasjon.reststs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class StsRestTemplateProducer {

    private final String uri;

    public StsRestTemplateProducer(@Value("${REST_STS.url}") String uri) {
        this.uri = uri;
    }

    @Primary
    @Bean(name = "stsRestTemplate")
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .rootUri(uri)
                .build();
    }
}
