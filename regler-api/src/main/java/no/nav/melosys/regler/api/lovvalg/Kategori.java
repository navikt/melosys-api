package no.nav.melosys.regler.api.lovvalg;

import static no.nav.melosys.regler.api.lovvalg.Alvorlighetsgrad.FEIL;

public enum Kategori {
    
    // Generelle feil
    TEKNISK_FEIL(FEIL, "Teknisk feil. Kontakt support."),
    IKKE_STOETTET(FEIL, "Det er ikke implementert maskinell støtte for denne forespørselen."),
    
    // Funksjonelle feil relatert til input
    FEIL_I_SOEKNAD(FEIL, "Ikke komplett eller inkonsistent søknad.");
    
    private final Alvorlighetsgrad alvorlighetsgrad;
    private final String melding;

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

    private Kategori(Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.melding = melding;
    }
 
}
