package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
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
    public DokgenConsumer dokgenConsumer(WebClient.Builder webClientBuilder,
                                         CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new DokgenConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(errorFilter("Kall mot dokumentgenereringstjeneste feilet."))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
