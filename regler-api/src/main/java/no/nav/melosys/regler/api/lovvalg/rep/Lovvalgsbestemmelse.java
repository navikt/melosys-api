package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.List;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Lovvalgsbestemmelse {
    
    /** Artikkel/lovhjemmel */
    public Artikkel artikkel;
    
    /** Liste med betingelser som bestemmer om artikkelen skal invokeres */
    @XmlElementWrapper(name = "betingelser")
    public List<Betingelse> betingelser;
    
}
