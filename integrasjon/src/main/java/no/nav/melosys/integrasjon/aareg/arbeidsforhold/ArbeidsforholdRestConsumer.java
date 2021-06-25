package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Objects.requireNonNull;

public class ArbeidsforholdRestConsumer implements RestConsumer {

    private final WebClient webClient;

    public ArbeidsforholdRestConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public ArbeidsforholdResponse finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsforholdQuery arbeidsforholdQuery) {
        ArbeidsforholdResponse.Arbeidsforhold[] arbeidsforholdResponse = hentArbeidsforhold(fnr, arbeidsforholdQuery);
        return new ArbeidsforholdResponse(arbeidsforholdResponse);
    }

    private ArbeidsforholdResponse.Arbeidsforhold[] hentArbeidsforhold(String fnr, ArbeidsforholdQuery arbeidsforholdQuery) {
        return requireNonNull( // TODO: sjekk om tjenesten kan returnere null;
            webClient.get().uri("", uriBuilder ->
                uriBuilder
                    .queryParam("regelverk", arbeidsforholdQuery.getRegelverk())
                    .queryParamIfPresent("arbeidsforholdType", arbeidsforholdQuery.getArbeidsforholdType())
                    .queryParamIfPresent("ansettelsesperiodeFom", arbeidsforholdQuery.getAnsettelsesperiodeFom())
                    .queryParamIfPresent("ansettelsesperiodeTom", arbeidsforholdQuery.getAnsettelsesperiodeTom())
                    // TODO: sjekk om metriks får url uten detaljer så det funker i grafana
                    .build())
                .header("Nav-Personident", fnr)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ArbeidsforholdResponse.Arbeidsforhold[].class)
                .block()
        );
    }
}
