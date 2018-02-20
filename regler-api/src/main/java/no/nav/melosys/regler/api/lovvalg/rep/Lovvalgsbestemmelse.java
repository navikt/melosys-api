package no.nav.melosys.regler.api.lovvalg.rep;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Lovvalgsbestemmelse {
    
    /** Artikkel/lovhjemmel */
    public Artikkel artikkel;
    
    /** Liste med betingelser som bestemmer om artikkelen skal invokeres */
    @XmlElementWrapper(name = "betingelser")
    @XmlElement(name = "betingelse")
    public List<Betingelse> betingelser;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lovvalgsbestemmelse that = (Lovvalgsbestemmelse) o;

        return artikkel == that.artikkel;
    }

    @Override
    public int hashCode() {
        return artikkel != null ? artikkel.hashCode() : 0;
    }
}
