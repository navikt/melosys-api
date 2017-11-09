package no.nav.melosys.regler.api.lovvalg.old;

import com.google.gson.Gson;

/**
 * DTO for varsler og feilmeldinger
 */
public class Feilmelding {

    /** Meldikngens kategori */
    public Kategori kategori;
    
    /** Teknisk melding */
    public String feilmelding;
 
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
