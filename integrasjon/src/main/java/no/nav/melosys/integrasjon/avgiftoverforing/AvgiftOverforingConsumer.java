package no.nav.melosys.integrasjon.avgiftoverforing;

import java.util.List;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

@Retryable
public class AvgiftOverforingConsumer {
    private final WebClient webClient;

    public AvgiftOverforingConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<AvgiftOverforingRepresentantDto> hentRepresentantListe(){
        return webClient.get()
            .uri("/v1/hent-representant-liste")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<AvgiftOverforingRepresentantDto>>(){})
            .block();
    }

    public AvgiftOverforingRepresentantDataDto hentRepresentant(String representantId){
        return webClient.get()
            .uri("/v1/hent-representant/{representantId}", representantId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AvgiftOverforingRepresentantDataDto.class)
            .block();
    }

}
