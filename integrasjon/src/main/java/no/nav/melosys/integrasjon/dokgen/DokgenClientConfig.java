package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.felles.WebClientUtilsKt.errorFilter;

@Configuration
public class DokgenClientConfig {
    private final String url;

    public DokgenClientConfig(@Value("${melosysdokgen.v1.url}") String url) {
        this.url = url;
    }

    @Bean
    public DokgenClient dokgenClient(WebClient.Builder webClientBuilder,
                                         CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new DokgenClient(
            webClientBuilder
                .baseUrl(url)
                .filter(errorFilter("Kall mot dokumentgenereringstjeneste feilet."))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
