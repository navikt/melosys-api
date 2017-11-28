package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.List;

/**
 * DTO for respons fra lovvalgtjenesten
 */
public class FastsettLovvalgReply {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    public List<Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    public List<Feilmelding> feilmeldinger;
    
}
