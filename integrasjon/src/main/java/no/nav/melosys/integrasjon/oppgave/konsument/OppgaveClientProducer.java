package no.nav.melosys.integrasjon.oppgave.konsument;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.felles.WebClientUtilsKt.errorFilter;

@Configuration
public class OppgaveClientProducer {
    private static final String CLIENT_NAME = "oppgave";
    private final String url;
    private final GenericAuthFilterFactory genericAuthFilterFactory;

    public OppgaveClientProducer(
        @Value("${OppgaveAPI_v1.url}") String url, GenericAuthFilterFactory genericAuthFilterFactory
    ) {
        this.url = url;
        this.genericAuthFilterFactory = genericAuthFilterFactory;
    }

    @Bean
    @Primary
    public OppgaveClient oppgaveClient(
        WebClient.Builder webClientBuilder, CorrelationIdOutgoingFilter correlationIdOutgoingFilter
    ) {
        return new OppgaveClient(
            webClientBuilder
                .defaultHeaders(this::defaultHeaders)
                .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot Oppgave feilet."))
                .baseUrl(url)
                .build()
        );
    }

    // v2 (beta) brukes kun for nøkkelord, som ikke finnes i v1. Samme API og Azure-scope,
    // kun versjonssegmentet i URL-en skiller.
    @Bean
    public OppgaveV2Client oppgaveV2Client(
        WebClient.Builder webClientBuilder, CorrelationIdOutgoingFilter correlationIdOutgoingFilter
    ) {
        return new OppgaveV2Client(
            webClientBuilder
                .defaultHeaders(this::defaultHeaders)
                .filter(genericAuthFilterFactory.getAzureFilter(CLIENT_NAME))
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot Oppgave v2 feilet."))
                .baseUrl(url.replace("/api/v1", "/api/v2"))
                .build()
        );
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
