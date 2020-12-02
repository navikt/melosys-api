package no.nav.melosys.integrasjon.trygdeavgift;

import no.nav.melosys.integrasjon.trygdeavgift.dto.BeregningsgrunnlagDto;
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TrygdeavgiftConsumer {

    private final WebClient webClient;

    public TrygdeavgiftConsumer(@Value("${melosystrygdeavgift.url}") String url) {
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeaders(this::defaultHeaders)
                .build();
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    public TrygdeavgiftDto beregnTrygdeavgift(BeregningsgrunnlagDto beregningsgrunnlagDto) {
        return webClient.post()
            .uri("/v1/beregn-trygdeavgift")
            .bodyValue(beregningsgrunnlagDto)
            .retrieve()
            .bodyToMono(TrygdeavgiftDto.class)
            .block();
    }
}
