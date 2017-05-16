package no.nav.melosys.saksflyt.api;

/**
 * Interface for alle maskinelle behandlingssteg.
 * 
 * Alle implementasjoner må være trådsikre.
 */
public interface Steg {
    
    /**
     * Kalles av arbeidertråder for å gi steget mulighet til å utføre arbeis.
     * Metoden skal kunne kalles parallelt.
     */
    public void finnSakOgutfoerSteg();
    
}
