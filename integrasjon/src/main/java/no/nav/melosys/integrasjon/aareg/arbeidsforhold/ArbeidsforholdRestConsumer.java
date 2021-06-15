package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.model.Arbeidsforhold;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Objects.requireNonNull;

public class ArbeidsforholdRestConsumer implements RestConsumer {

    private final WebClient webClient;

    public ArbeidsforholdRestConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public ArbeidsfoholdResponse finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsforholdQuery arbeidsfoholdQuery) {
        Arbeidsforhold[] arbeidsforholdResponse = hentArbeidsfohold(fnr, arbeidsfoholdQuery);
        return new ArbeidsfoholdResponse(arbeidsforholdResponse);
    }

    private Arbeidsforhold[] hentArbeidsfohold(String fnr, ArbeidsforholdQuery arbeidsfoholdQuery) {
        return requireNonNull(
            webClient.get().uri("", uriBuilder ->
                uriBuilder
                    .queryParam("regelverk", arbeidsfoholdQuery.getRegelverk())
                    .queryParamIfPresent("arbeidsforholdType", arbeidsfoholdQuery.getArbeidsforholdType())
                    // TODO: add all more params
                    .build())
                .header("Nav-Personident", fnr)
                .header(HttpHeaders.AUTHORIZATION, getAuth())
                .header("Nav-Consumer-Token", getAuth())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Arbeidsforhold[].class)
                .block()
        );
    }
}
