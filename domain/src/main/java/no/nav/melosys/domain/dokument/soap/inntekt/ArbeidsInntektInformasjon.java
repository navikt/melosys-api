package no.nav.melosys.domain.dokument.soap.inntekt;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe = new ArrayList<>();

    @JsonProperty("arbeidsforholdFrilanserListe")
    public List<ArbeidsforholdFrilanser> arbeidsforholdListe = new ArrayList<>();

    public List<Inntekt> getInntektListe() {
        return inntektListe;
    }

    public List<ArbeidsforholdFrilanser> getArbeidsforholdListe() {
        return arbeidsforholdListe;
    }

}
