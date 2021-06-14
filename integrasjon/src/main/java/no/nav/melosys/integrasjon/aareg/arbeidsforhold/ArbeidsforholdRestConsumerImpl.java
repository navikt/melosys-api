package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import org.springframework.web.reactive.function.client.WebClient;

public class ArbeidsforholdRestConsumerImpl implements ArbeidsforholdRestConsumer {

    private final WebClient webClient;

    public ArbeidsforholdRestConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    // regelverk
    public String finnArbeidsforholdPrArbeidstaker(String navArbeidsforholdId) {
        return "TODO: get from rest service";
    }
}
