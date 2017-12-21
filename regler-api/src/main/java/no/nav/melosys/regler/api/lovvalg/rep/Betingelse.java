package no.nav.melosys.regler.api.lovvalg.rep;

/**
 * En betingelse som (delvis) kvalifiserer for en lovanvendelse.
 */
public class Betingelse {
    
    /** Argumentet til betingelsen */
    public Argument argument;
    
    /** Kravet argumentet må tilfredsstille */
    public String krav;
    
    /** Resultatet av evalueringen */
    public Resultat resultat;

}
