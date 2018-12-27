package no.nav.melosys.integrasjon.eux.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EuxRestTemplateProducer {

    @Value("${euxbasis.v1.url}")
    private String uri;

    @Bean
    public RestTemplate euxRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .rootUri(uri)
                .build();
    }
}
