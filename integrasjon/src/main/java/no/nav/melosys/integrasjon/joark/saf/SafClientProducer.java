package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.felles.WebClientUtilsKt.errorFilter;

@Configuration
public class SafClientProducer {
    private static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";
    private static final String CLIENT_NAME = "saf";

    private final String url;
    private final GenericAuthFilterFactory genericAuthFilterFactory;

    public SafClientProducer(
        @Value("${SAF.url}") String url, GenericAuthFilterFactory genericAuthFilterFactory

    ) {
        this.url = url;
        this.genericAuthFilterFactory = genericAuthFilterFactory;
    }

    @Bean
    public SafClient safClient(WebClient.Builder webClientBuilder,
                                   CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new SafClient(
            webClientBuilder
                .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
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
