package no.nav.melosys.regler.api.lovvalg;

import java.util.List;

import com.google.gson.Gson;

/**
 * DTO for respons fra lovvalgtjenesten
 */
public class FastsettLovvalgRespons {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    public List<Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    public List<Feilmelding> feilmeldinger;
    
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
