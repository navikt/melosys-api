package no.nav.melosys.regler.api.lovvalg;

<<<<<<< HEAD
import com.google.gson.Gson;

=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
/**
 * DTO for varsler og feilmeldinger
 */
public class Feilmelding {

<<<<<<< HEAD
    /** Meldikngens kategori */
    public Kategori kategori;
    
    /** Teknisk melding */
    public String feilmelding;
 
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

=======
    /** Meldikngens kategori (inneholder bl.a. funksjonell melding */
    public Kategori kategori;
    
    /** Angivelse av felter/attributter meldingen gjelder for (hvis noen) */
    public String[] felter;

    /** Teknisk melding */
    public String feilmelding;
    
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
}
