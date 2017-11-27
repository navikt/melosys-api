package no.nav.melosys.regler.api.lovvalg.rep;

/**
 * En betingelse som (delvis) kvalifiserer for en lovanvendelse.
 */
public class Betingelse {
    
    /** Kravet som må oppfylles */
    public Argument krav;
    
    /** Resultatet av evalueringen */
    public Resultat resultat;

}
