package no.nav.melosys.integrasjon.eux.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EuxRestTemplateProducer {

    private final String uri;

    public EuxRestTemplateProducer(@Value("${euxbasis.v1.url}") String uri) {
        this.uri = uri;
    }

    @Bean(name = "euxRestTemplate")
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .rootUri(uri)
                .build();
    }
}
