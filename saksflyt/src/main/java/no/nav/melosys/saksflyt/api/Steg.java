package no.nav.melosys.saksflyt.api;

/**
 * Interface for alle maskinelle behandlingssteg.
 * <p>
 * Alle implementasjoner må være trådsikre.
 */
public interface Steg {

    /**
     * Kalles av arbeidertråder for å gi steget mulighet til å utføre arbeid. Metoden skal kunne kalles parallelt.
     */
    public void finnBehandlingOgUtfoerSteg();

}
