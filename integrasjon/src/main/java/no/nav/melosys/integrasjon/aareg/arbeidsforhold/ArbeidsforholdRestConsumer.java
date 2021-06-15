package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

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

    public ArbeidsfoholdResponse[] finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsfoholdQuery arbeidsfoholdQuery) {
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
                .bodyToMono(ArbeidsfoholdResponse[].class)
                .block()
        );
    }
}
