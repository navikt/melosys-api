package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.AutoContextExchangeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SafConsumerProducer.class);
    private static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";

    private final String url;

    public SafConsumerProducer(@Value("${SAF.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public SafConsumer safConsumer(WebClient.Builder webClientBuilder, AutoContextExchangeFilter autoContextExchangeFilter) {
        return new SafConsumerImpl(
            webClientBuilder
                .filter(autoContextExchangeFilter)
                .defaultHeaders(this::defaultHeaders)
                .baseUrl(url)
                .build()
        );
    }

    @Bean
    @Qualifier("system")
    public SafConsumer safSystemConsumer(WebClient.Builder webClientBuilder, AutoContextExchangeFilter systemContextExchangeFilter) {
        log.info("no need for @Qualifier(\"system\") provided while we clean up");
        return safConsumer(webClientBuilder, systemContextExchangeFilter);
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(NAV_CONSUMER_ID, "melosys");
    }
}
