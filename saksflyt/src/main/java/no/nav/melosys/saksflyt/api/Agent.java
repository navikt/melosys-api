package no.nav.melosys.saksflyt.api;

/**
 * Interface for alle agenter som utfører maskinelle steg.
 * 
 * Alle implementasjoner må være trådsikre.
 */
public interface Agent {

    /**
     * Kalles av arbeidertråder for å gi agenten mulighet til å utføre arbeid. Metoden skal kunne kalles parallelt.
     * Implementasjonen må sørge for at et kall ikke tar for lang tid (i alle fall ikke mer enn 1 minutt).
     */
    public void finnProsessinstansOgUtfoerSteg();

}
