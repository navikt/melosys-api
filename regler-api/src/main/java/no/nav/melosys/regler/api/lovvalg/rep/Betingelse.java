package no.nav.melosys.regler.api.lovvalg.rep;

/**
 * En betingelse som (delvis) kvalifiserer for en lovanvendelse.
 */
public class Betingelse {
    
    /** Funksjonell beskrivelse av betingelsen */
    public Argument argument;
    
    /** Kravet som må oppfylles */
    public String krav;
    
    /** Verdien som er lagt til grunn */
    public Object verdi;
    
    /** Resultatet av evalueringen */
    public Resultat resultat;

}
