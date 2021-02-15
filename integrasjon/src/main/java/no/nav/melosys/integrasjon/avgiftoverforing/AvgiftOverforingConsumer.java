package no.nav.melosys.integrasjon.avgiftoverforing;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class AvgiftOverforingConsumer {
    private final WebClient webClient;

    public AvgiftOverforingConsumer(@Value("${melosysavgiftoverforing.url}") String url){
        this.webClient = WebClient.create(url);
    }

    public AvgiftOverforingRepresentantDto[] hentRepresentantListe(){
        return webClient.get()
            .uri("/v1/hent-representant-liste")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AvgiftOverforingRepresentantDto[].class)
            .block();
    }

    public AvgiftOverforingRepresentantDataDto hentRepresentant(String representantId){
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/hent-representant/{representantId}")
                .build(representantId))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AvgiftOverforingRepresentantDataDto.class)
            .block();
    }

}
