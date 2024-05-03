package no.nav.melosys.integrasjon.eessi;

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
public class EessiConsumerProducerConfig implements WebClientConfig {
    private static final String CLIENT_NAME = "melosys-eessi";
    private final String url;
    private final GenericAuthFilterFactory genericAuthFilterFactory;

    public EessiConsumerProducerConfig(
        @Value("${MelosysEessi.url}") String url, GenericAuthFilterFactory genericAuthFilterFactory
    ) {
        this.url = url;
        this.genericAuthFilterFactory = genericAuthFilterFactory;
    }

    @Bean
    public EessiConsumer melosysEessiConsumer(
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter, WebClient.Builder webClientBuilder
    ) {
        return new EessiConsumerImpl(webClientBuilder
            .baseUrl(url)
            .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
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
