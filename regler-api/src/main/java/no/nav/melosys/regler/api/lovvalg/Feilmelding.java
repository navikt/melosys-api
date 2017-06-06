package no.nav.melosys.regler.api.lovvalg;

/**
 * DTO for varsler og feilmeldinger
 */
public class Feilmelding {

    /** Meldikngens kategori (inneholder bl.a. funksjonell melding */
    public Kategori kategori;
    
    /** Angivelse av felter/attributter meldingen gjelder for (hvis noen) */
    public String[] felter;

    /** Teknisk melding */
    public String feilmelding;
    
}
