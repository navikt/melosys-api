package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.List;
import java.util.Map;

/**
 * DTO for respons fra lovvalgtjenesten
 */
public class FastsettLovvalgReply {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    public Map<Artikkel, Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    public List<Feilmelding> feilmeldinger;
    
}
