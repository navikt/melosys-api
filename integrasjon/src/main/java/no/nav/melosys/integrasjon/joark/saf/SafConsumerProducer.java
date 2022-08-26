package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SafConsumerProducer implements WebClientConfig {
    private static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";

    private final String url;

    public SafConsumerProducer(@Value("${SAF.url}") String url) {
        this.url = url;
    }

    @Bean
    public SafConsumer safConsumer(WebClient.Builder webClientBuilder,
                                   GenericContextExchangeFilter genericContextExchangeFilter,
                                   CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new SafConsumerImpl(
            webClientBuilder
                .filter(genericContextExchangeFilter)
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot SAF feilet."))
                .defaultHeaders(this::defaultHeaders)
                .baseUrl(url)
                .build()
        );
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(NAV_CONSUMER_ID, "melosys");
    }
}
