package no.nav.melosys.regler.api.lovvalg;

import static no.nav.melosys.regler.api.lovvalg.Alvorlighetsgrad.FEIL;

public enum Kategori {
    
<<<<<<< HEAD
    // Generelle feil
    TEKNISK_FEIL(FEIL, "Teknisk feil. Kontakt support."),
    IKKE_STOETTET(FEIL, "Det er ikke implementert maskinell støtte for denne forespørselen."),
    
    // Funksjonelle feil relatert til input
    FEIL_I_SOEKNAD(FEIL, "Ikke komplett eller inkonsistent søknad.");
    
    private final Alvorlighetsgrad alvorlighetsgrad;
    private final String melding;
=======
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
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb

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

<<<<<<< HEAD
    private Kategori(Alvorlighetsgrad alvorlighetsgrad, String melding) {
=======
    private Kategori(int meldingNr, Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.meldingNr = meldingNr;
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.melding = melding;
    }
 
}
