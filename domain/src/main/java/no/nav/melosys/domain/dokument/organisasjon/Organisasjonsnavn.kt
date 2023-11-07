package no.nav.melosys.domain.dokument.organisasjon;


import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import no.nav.melosys.domain.dokument.felles.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Organisasjonsnavn {

    public Periode bruksperiode;

    public Periode gyldighetsperiode;
    
    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    public List<String> navn = new ArrayList<>();

    public String redigertNavn;

    public Periode getBruksperiode() {
        return bruksperiode;
    }

    public Periode getGyldighetsperiode() {
        return gyldighetsperiode;
    }

    public List<String> getNavn() {
        return navn;
    }

    public String getRedigertNavn() {
        return redigertNavn;
    }

}
