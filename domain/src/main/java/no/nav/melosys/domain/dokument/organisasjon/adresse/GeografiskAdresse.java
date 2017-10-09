package no.nav.melosys.domain.dokument.organisasjon.adresse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;

import no.nav.melosys.domain.dokument.felles.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({
        SemistrukturertAdresse.class, Gateadresse.class
})
public class GeografiskAdresse {

    protected Periode bruksperiode;

    protected Periode gyldighetsperiode;
    
    private String landkode;

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

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String value) {
        this.landkode = value;
    }

}
