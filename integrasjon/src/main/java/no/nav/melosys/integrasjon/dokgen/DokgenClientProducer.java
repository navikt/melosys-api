package no.nav.melosys.integrasjon.dokgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
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
        ObjectMapper dokgenObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new DokgenClient(
            webClientBuilder
                .baseUrl(url)
                .codecs(configurer -> configurer.defaultCodecs()
                    .jackson2JsonEncoder(new Jackson2JsonEncoder(dokgenObjectMapper)))
                .filter(errorFilter("Kall mot dokumentgenereringstjeneste feilet."))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
