package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DokgenConsumerProducer implements WebClientConfig {
    private final String url;

    public DokgenConsumerProducer(@Value("${melosysdokgen.v1.url}") String url) {
        this.url = url;
    }

    @Bean
    public DokgenConsumer dokgenConsumer(WebClient.Builder webClientBuilder) {
        return new DokgenConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(errorFilter("Kall mot dokumentgenereringstjeneste feilet. %s - %s"))
                .build()
        );
    }
}
