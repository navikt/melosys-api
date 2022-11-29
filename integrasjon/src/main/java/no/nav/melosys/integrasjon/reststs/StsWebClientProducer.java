package no.nav.melosys.integrasjon.reststs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class StsWebClientProducer {

    private final String uri;

    public StsWebClientProducer(@Value("${REST_STS.url}") String uri) {
        this.uri = uri;
    }

    @Bean
    public WebClient webClient(WebClient.Builder webclientBuilder) {
        return webclientBuilder
            .baseUrl(uri)
            .build();
    }


}
