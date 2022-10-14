package no.nav.melosys.integrasjon.oppgave.konsument;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OppgaveConsumerProducer implements WebClientConfig {
    private final String url;

    public OppgaveConsumerProducer(@Value("${OppgaveAPI_v1.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public OppgaveConsumer oppgaveConsumer(WebClient.Builder webClientBuilder,
                                           OppgaveGenericContextExchangeFilter oppgaveGenericContextExchangeFilter,
                                           CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new OppgaveConsumerImpl(
            webClientBuilder
                .defaultHeaders(this::defaultHeaders)
                .filter(oppgaveGenericContextExchangeFilter)
                .filter(correlationIdOutgoingFilter)
                .filter(errorFilter("Kall mot Oppgave feilet."))
                .baseUrl(url)
                .build()
        );
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
