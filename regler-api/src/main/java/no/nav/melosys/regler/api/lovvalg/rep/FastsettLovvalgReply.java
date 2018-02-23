package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * DTO for respons fra lovvalgtjenesten
 */
@XmlRootElement
public class FastsettLovvalgReply {
    
    /** Liste med bestemmelser (artikler) søknaden er vurdert mot */
    @XmlElementWrapper(name="lovvalgsbestemmelser")
    @XmlElement(name = "lovvalgsbestemmelse")
    public List<Lovvalgsbestemmelse> lovvalgsbestemmelser;
    
    /** Liste med evt. feilmeldinger */
    @XmlElementWrapper(name="feilmeldinger")
    @XmlElement(name = "feilmelding")
    public List<Feilmelding> feilmeldinger;
}
