package no.nav.melosys.regler.api.lovvalg;

import java.util.List;

<<<<<<< HEAD
import com.google.gson.Gson;

=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
/**
 * DTO for respons fra lovvalgtjenesten
 */
public class FastsettLovvalgRespons {
    
<<<<<<< HEAD
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    public List<Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    public List<Feilmelding> feilmeldinger;
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
=======
    public List<Feilmelding> feilmeldinger;
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb

}
