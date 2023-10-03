package no.nav.melosys.domain.dokument.inntekt;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe = new ArrayList<>();

    @JsonProperty("arbeidsforholdFrilanserListe")
    @NotNull
    public List<ArbeidsforholdFrilanser> arbeidsforholdListe = new ArrayList<>();

    public List<Inntekt> getInntektListe() {
        return inntektListe;
    }

    public List<ArbeidsforholdFrilanser> getArbeidsforholdListe() {
        return arbeidsforholdListe;
    }

}
