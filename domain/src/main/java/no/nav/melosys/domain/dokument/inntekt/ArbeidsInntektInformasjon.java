package no.nav.melosys.domain.dokument.inntekt;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe = new ArrayList<>();

    @JsonIgnore
    public List<Forskuddstrekk> forskuddstrekkListe = new ArrayList<>();

    @JsonIgnore
    public List<Fradrag> fradragListe = new ArrayList<>();

    @JsonProperty("arbeidsforholdFrilanserListe")
    public List<ArbeidsforholdFrilanser> arbeidsforholdListe = new ArrayList<>();
    
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