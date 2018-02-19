package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * DTO for respons fra lovvalgtjenesten
 */
@XmlRootElement
public class FastsettLovvalgReply {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    @XmlTransient
    public Map<Artikkel, Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    @XmlElementWrapper(name="feilmeldinger")
    public List<Feilmelding> feilmeldinger;

    @XmlElementWrapper(name="lovvalgsbestemmelser")
    public Collection<Lovvalgsbestemmelse> getLovvalgsbestemmelser() {
        return lovvalgsbestemmelser == null ? Collections.EMPTY_LIST : lovvalgsbestemmelser.values();
    }

    @XmlElementWrapper(name="lovvalgsbestemmelser")
    public void setLovvalgsbestemmelser(List<Lovvalgsbestemmelse> lovvalgsbestemmelser) {
        this.lovvalgsbestemmelser = new HashMap<>();
        lovvalgsbestemmelser.forEach(lovvalgsbestemmelse -> {
            this.lovvalgsbestemmelser.put(lovvalgsbestemmelse.artikkel, lovvalgsbestemmelse);
        });
    }
}
