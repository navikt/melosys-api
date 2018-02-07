package no.nav.melosys.domain.dokument.inntekt;

import java.util.List;

public class ArbeidsInntektInformasjon {

    public List<Inntekt> inntektListe;

    public List<Forskuddstrekk> forskuddstrekkListe;

    public List<Fradrag> fradragListe;

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