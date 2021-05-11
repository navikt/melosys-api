package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter;
import no.nav.melosys.integrasjon.felles.UserContextExchangeFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SafConsumerProducer {
    private static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";

    private final String url;

    public SafConsumerProducer(@Value("${SAF.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public SafConsumer safConsumer(WebClient.Builder webClientBuilder, UserContextExchangeFilter userContextExchangeFilter) {
        return new SafConsumerImpl(
            webClientBuilder
                .filter(userContextExchangeFilter)
                .defaultHeaders(this::defaultHeaders)
                .baseUrl(url)
                .build()
        );
    }

    @Bean
    @Qualifier("system")
    public SafConsumer safSystemConsumer(WebClient.Builder webClientBuilder, SystemContextExchangeFilter systemContextExchangeFilter) {
        return new SafConsumerImpl(
            webClientBuilder
                .filter(systemContextExchangeFilter)
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
