package no.nav.melosys.integrasjon.avgiftoverforing;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AvgiftOverforingConsumerProducer implements WebClientConfig {
    private final String url;

    public AvgiftOverforingConsumerProducer(@Value("${melosysavgiftoverforing.url}") String url) {
        this.url = url;
    }

    @Bean
    public AvgiftOverforingConsumer avgiftOverforingConsumer(WebClient.Builder webClientBuilder) {
        return new AvgiftOverforingConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(errorFilter("Kall mot avgift-overføring feilet. %s - %s"))
                .build()
        );
    }
}
