package no.nav.melosys.integrasjon.pdl;

import java.util.Collections;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PDLConsumerProducer implements WebClientConfig {
    private static final String TEMA_HEADER = "Tema";
    private static final String TEMA_HEADER_MEDLEMSKAP_VERDI = "MED";

    @Bean
    public PDLConsumer pdlConsumer(WebClient.Builder webclientBuilder,
                                   @Value("${PDL.url}") String pdlUrl,
                                   PDLAuthFilter pdlSystemAuthFilter) {
        return new PDLConsumerImpl(
            webclientBuilder(webclientBuilder, pdlUrl)
                .filter(pdlSystemAuthFilter)
                .filter(errorFilter("Kall mot PDL feilet."))
                .build()
        );
    }

    private WebClient.Builder webclientBuilder(WebClient.Builder webclientBuilder, String pdlUrl) {
        return webclientBuilder
            .baseUrl(pdlUrl)
            .defaultHeaders(this::defaultHeaders);
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(TEMA_HEADER, TEMA_HEADER_MEDLEMSKAP_VERDI);
    }
}
