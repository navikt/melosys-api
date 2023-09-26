package no.nav.melosys.domain.dokument.inntekt;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe = new ArrayList<>();

    @JsonProperty("arbeidsforholdFrilanserListe")
    public List<ArbeidsforholdFrilanser> arbeidsforholdListe = new ArrayList<>();

    public List<Inntekt> getInntektListe() {
        if (inntektListe == null) {
            // det må gjøres siden inntektListe er satt til null i konvertering og lagret i databasen
            inntektListe = new ArrayList<>();
        }
        return inntektListe;
    }

    public List<ArbeidsforholdFrilanser> getArbeidsforholdListe() {
        return arbeidsforholdListe;
    }

}
