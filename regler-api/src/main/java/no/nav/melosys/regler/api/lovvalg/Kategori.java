package no.nav.melosys.regler.api.lovvalg;

import static no.nav.melosys.regler.api.lovvalg.Alvorlighetsgrad.FEIL;

public enum Kategori {
    
    TEKNISK_FEIL(1000, FEIL, "Teknisk feil. Kontakt support."),
    IKKE_STOETTET(1001, FEIL, "Det er ikke implementert maskinell støtte for denne forespørselen.");
    
    private int meldingNr;
    private Alvorlighetsgrad alvorlighetsgrad;
    private String melding;

    /**
     * @return Kategoriens unike meldingsnummer
     */
    public int getMeldingNr() {
        return meldingNr;
    }

    /**
     * @return Meldingens alvorlighetsgrad
     */
    public Alvorlighetsgrad getAlvorlighetsgrad() {
        return alvorlighetsgrad;
    }
    
    /**
     * @return Meldingens funksjonelle tekst.
     */
    public String getMelding() {
        return melding;
    }

    private Kategori(int meldingNr, Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.meldingNr = meldingNr;
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.melding = melding;
    }
 
}
