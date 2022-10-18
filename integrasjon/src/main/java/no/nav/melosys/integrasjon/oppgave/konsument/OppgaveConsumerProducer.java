package no.nav.melosys.integrasjon.oppgave.konsument;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
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
    private static final String CLIENT_NAME = "oppgave";
    private final String url;
    private final GenericAuthFilterFactory genericAuthFilterFactory;

    public OppgaveConsumerProducer(
        @Value("${OppgaveAPI_v1.url}") String url, GenericAuthFilterFactory genericAuthFilterFactory
    ) {
        this.url = url;
        this.genericAuthFilterFactory = genericAuthFilterFactory;
    }

    @Bean
    @Primary
    public OppgaveConsumer oppgaveConsumer(
        WebClient.Builder webClientBuilder, CorrelationIdOutgoingFilter correlationIdOutgoingFilter
    ) {
        return new OppgaveConsumerImpl(
            webClientBuilder
                .defaultHeaders(this::defaultHeaders)
                .filter(genericAuthFilterFactory.getFilter(CLIENT_NAME))
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
