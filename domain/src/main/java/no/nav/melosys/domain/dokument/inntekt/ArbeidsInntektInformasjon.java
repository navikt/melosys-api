package no.nav.melosys.domain.dokument.inntekt;

import java.util.List;

public class ArbeidsInntektInformasjon {

    private List<Inntekt> inntektListe;

    private List<Forskuddstrekk> forskuddstrekkListe;

    private List<Fradrag> fradragListe;


    public List<Inntekt> getInntektListe() {
        return inntektListe;
    }

    public void setInntektListe(List<Inntekt> inntektListe) {
        this.inntektListe = inntektListe;
    }

    public List<Forskuddstrekk> getForskuddstrekkListe() {
        return forskuddstrekkListe;
    }

    public void setForskuddstrekkListe(List<Forskuddstrekk> forskuddstrekkListe) {
        this.forskuddstrekkListe = forskuddstrekkListe;
    }

    public List<Fradrag> getFradragListe() {
        return fradragListe;
    }

    public void setFradragListe(List<Fradrag> fradragListe) {
        this.fradragListe = fradragListe;
    }
}
