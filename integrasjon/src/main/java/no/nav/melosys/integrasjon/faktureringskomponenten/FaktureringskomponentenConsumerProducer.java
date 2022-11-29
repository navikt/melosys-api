package no.nav.melosys.integrasjon.faktureringskomponenten;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FaktureringskomponentenConsumerProducer implements WebClientConfig {

    private final String url;
    private final GenericAuthFilterFactory genericAuthFilterFactory;

    public FaktureringskomponentenConsumerProducer(@Value("${faktureringskomponenten.url}") String url,
                                                   GenericAuthFilterFactory genericAuthFilterFactory) {
        this.url = url;
        this.genericAuthFilterFactory = genericAuthFilterFactory;
    }

    @Bean
    public FaktureringskomponentenConsumer faktureringskomponentenConsumer(
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        WebClient.Builder webClientBuilder
    ) {
        return new FaktureringskomponentenConsumer(webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getFilter("faktureringskomponenten"))
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot Faktureringskomponenten feilet"))
            .defaultHeaders(this::defaultHeaders)
            .build()) {
        };
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
