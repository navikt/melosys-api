package no.nav.melosys.domain.dokument.organisasjon;


import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import no.nav.melosys.domain.dokument.felles.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
public class Organisasjonsnavn {

    protected Periode bruksperiode;

    protected Periode gyldighetsperiode;
    
    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    private List<String> navn;

    protected String redigertNavn;

    public Periode getBruksperiode() {
        return bruksperiode;
    }

    public void setBruksperiode(Periode bruksperiode) {
        this.bruksperiode = bruksperiode;
    }

    public Periode getGyldighetsperiode() {
        return gyldighetsperiode;
    }

    public void setGyldighetsperiode(Periode gyldighetsperiode) {
        this.gyldighetsperiode = gyldighetsperiode;
    }

    public List<String> getNavn() {
        return navn;
    }

    public void setNavn(List<String> value) {
        this.navn = value;
    }

    public String getRedigertNavn() {
        return redigertNavn;
    }

    public void setRedigertNavn(String value) {
        this.redigertNavn = value;
    }

}
