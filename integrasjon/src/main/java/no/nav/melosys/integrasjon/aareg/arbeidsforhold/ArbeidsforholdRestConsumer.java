package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.util.List;

import no.nav.melosys.integrasjon.felles.WebClientConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

@Retryable
public class ArbeidsforholdRestConsumer implements WebClientConfig {

    private final WebClient webClient;

    public ArbeidsforholdRestConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public ArbeidsforholdResponse finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsforholdQuery arbeidsforholdQuery) {
        return new ArbeidsforholdResponse(hentArbeidsforhold(fnr, arbeidsforholdQuery));
    }

    private List<ArbeidsforholdResponse.Arbeidsforhold> hentArbeidsforhold(String fnr, ArbeidsforholdQuery arbeidsforholdQuery) {
        return webClient.get().uri("", uriBuilder ->
            uriBuilder
                .queryParam("regelverk", arbeidsforholdQuery.getRegelverk())
                .queryParamIfPresent("arbeidsforholdType", arbeidsforholdQuery.getArbeidsforholdType())
                .queryParamIfPresent("ansettelsesperiodeFom", arbeidsforholdQuery.getAnsettelsesperiodeFom())
                .queryParamIfPresent("ansettelsesperiodeTom", arbeidsforholdQuery.getAnsettelsesperiodeTom())
                .build())
                // Om vi ønsker å se request med mer detaljer i grafana må vi gjøre det samme som er gjort i MedlemskapRestConsumer
                // Nå ser vi bare request på host
            .header("Nav-Personident", fnr)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<ArbeidsforholdResponse.Arbeidsforhold>>(){})
            .block();
    }
}
