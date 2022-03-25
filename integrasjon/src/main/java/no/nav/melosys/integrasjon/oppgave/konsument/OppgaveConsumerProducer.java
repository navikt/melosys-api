package no.nav.melosys.integrasjon.oppgave.konsument;

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
public class OppgaveConsumerProducer {
    private static final Logger log = LoggerFactory.getLogger(OppgaveConsumerProducer.class);

    private final String url;

    public OppgaveConsumerProducer(@Value("${OppgaveAPI_v1.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public OppgaveConsumer oppgaveConsumer(WebClient.Builder webClientBuilder, AutoContextExchangeFilter autoContextExchangeFilter) {
        return new OppgaveConsumerImpl(
            webClientBuilder
                .filter(autoContextExchangeFilter)
                .defaultHeaders(this::defaultHeaders)
                .baseUrl(url)
                .build()
        );
    }

    @Bean
    @Qualifier("system")
    public OppgaveConsumer oppgaveSystemConsumer(WebClient.Builder webClientBuilder, AutoContextExchangeFilter autoContextExchangeFilter) {
        log.info("no need for @Qualifier(\"system\") provided while we clean up");
        return oppgaveConsumer(webClientBuilder, autoContextExchangeFilter);
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
