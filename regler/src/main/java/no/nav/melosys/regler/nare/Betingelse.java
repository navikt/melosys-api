package no.nav.melosys.regler.nare;

import no.nav.melosys.regler.api.lovvalg.Resultat;

/**
 * Felles klasse for betingelser.
 */
public abstract class Betingelse {
    
    private String beskrivelse;

    protected Betingelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }
    
    /**
     * Henter søknaden fra kjørekontekst og evaluerer den for denne betingelsen.
     */
    public abstract Resultat evaluer();

    /**
     * Funksjonell beskrivelse av betingelsen.
     */
    public String getBeskrivelse() {
        return beskrivelse;
    }
    
}
