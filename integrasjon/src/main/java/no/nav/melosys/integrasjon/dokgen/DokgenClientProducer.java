package no.nav.melosys.integrasjon.dokgen;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.felles.WebClientUtilsKt.errorFilter;

@Configuration
public class DokgenClientProducer {
    private final String url;

    public DokgenClientProducer(@Value("${melosysdokgen.v1.url}") String url) {
        this.url = url;
    }

    @Bean
    public DokgenClient dokgenClient(WebClient.Builder webClientBuilder,
                                         CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        // Egen ObjectMapper uten MelosysModule for dokgen-kall.
        // MelosysModule sin KodeSerializer konverterer Kodeverk-enums til {"kode":"...","term":"..."}
        // objekter, men dokgen forventer enkle strenger.
        JsonMapper dokgenObjectMapper = JsonMapper.builder().build();

        return new DokgenClient(
            webClientBuilder
                .baseUrl(url)
                .codecs(configurer -> configurer.defaultCodecs()
                    .jacksonJsonEncoder(new JacksonJsonEncoder(dokgenObjectMapper)))
                .filter(errorFilter("Kall mot dokumentgenereringstjeneste feilet."))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
