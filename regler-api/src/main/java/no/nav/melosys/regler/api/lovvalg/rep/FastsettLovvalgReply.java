package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.*;

/**
 * DTO for respons fra lovvalgtjenesten
 */
public class FastsettLovvalgReply {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    public Map<Artikkel, Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    public List<Feilmelding> feilmeldinger;

    // FIXME: Midlertidig løsning for å støtte kontrakten med frontend
    public Collection<Lovvalgsbestemmelse> getLovvalgsbestemmelser() {
        return lovvalgsbestemmelser.values();
    }

    public void setLovvalgsbestemmelser(List<Lovvalgsbestemmelse> lovvalgsbestemmelser) {
        this.lovvalgsbestemmelser = new HashMap<>();
        lovvalgsbestemmelser.forEach(lovvalgsbestemmelse -> {
            this.lovvalgsbestemmelser.put(lovvalgsbestemmelse.artikkel, lovvalgsbestemmelse);
        });
    }
}
