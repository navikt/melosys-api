package no.nav.melosys.domain.dokument.inntekt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe;

    @JsonIgnore
    public List<Forskuddstrekk> forskuddstrekkListe;

    @JsonIgnore
    public List<Fradrag> fradragListe;

    @JsonProperty("arbeidsforholdFrilanserListe")
    public List<ArbeidsforholdFrilanser> arbeidsforholdListe;
    
    public List<Inntekt> getInntektListe() {
        return inntektListe;
    }

    public List<Forskuddstrekk> getForskuddstrekkListe() {
        return forskuddstrekkListe;
    }

    public List<Fradrag> getFradragListe() {
        return fradragListe;
    }

    public List<ArbeidsforholdFrilanser> getArbeidsforholdListe() {
        return arbeidsforholdListe;
    }

}