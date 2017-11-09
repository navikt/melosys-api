package no.nav.melosys.regler.api.lovvalg.old;

/**
 * En betingelse (kriterie) som (delvis) kvalifiserer for en lovanvendelse.
 * 
 * FIXME (farjam): Denne klassen trenger et navn som gjenspeiler at den er både betingelse og resultat av regelkjøringen.
 */
public class Betingelse {
    
    /** Funksjonell beskrivelse av kriteriet */
    public String beskrivelse;
    
    /** Resultatet fra regelmodulen */
    public Resultat resultat;

}
