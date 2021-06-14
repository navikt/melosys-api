package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Objects.requireNonNull;

public class ArbeidsforholdRestConsumerImpl implements ArbeidsforholdRestConsumer, RestConsumer {

    private final WebClient webClient;

    public ArbeidsforholdRestConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public ArbeidsfoholdResponse[] finnArbeidsforholdPrArbeidstaker(String fnr, ArbeidsfoholdQuery arbeidsfoholdQuery) {
        return requireNonNull(
            webClient.get().uri("", uriBuilder ->
                uriBuilder
                    .queryParam("arbeidsforholdType", arbeidsfoholdQuery.arbeidsforholdType) // TODO: add all supported params
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
