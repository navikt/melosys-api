package no.nav.melosys.regler.api.lovvalg.rep;

/**
 * DTO for varsler og feilmeldinger
 */
public class Feilmelding {

    /** Meldikngens kategori */
    public Kategori kategori;

    // FIXME: Frontend har behov for Alvorlighetsgrad("FEIL", "VARSEL", "INFO") Implementeres i regelmodul?
    public String alvorlighetsgrad;
    
    /** Teknisk melding */
    public String melding;
 
}
