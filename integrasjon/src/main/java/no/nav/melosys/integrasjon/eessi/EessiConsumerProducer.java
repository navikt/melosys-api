package no.nav.melosys.integrasjon.eessi;

import no.nav.melosys.integrasjon.felles.EessiGenericContextExchangeFilter;
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
public class EessiConsumerProducer implements WebClientConfig {

    private final String url;

    public EessiConsumerProducer(@Value("${MelosysEessi.url}") String url) {
        this.url = url;
    }

    @Bean
    public EessiConsumer melosysEessiConsumer(EessiGenericContextExchangeFilter eessiGenericContextExchangeFilter,
                                              CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
                                              WebClient.Builder webClientBuilder
    ) {
        return new EessiConsumerImpl(webClientBuilder
            .baseUrl(url)
            .filter(eessiGenericContextExchangeFilter)
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Kall mot eessi feilet"))
            .defaultHeaders(this::defaultHeaders)
            .build());
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
