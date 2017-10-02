package no.nav.melosys.domain.dokument.organisasjon.adresse;

import java.time.LocalDate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.OffsetDateTimeToLocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({
        SemistrukturertAdresse.class, Gateadresse.class
})
public class GeografiskAdresse {

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate fomGyldighetsperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate tomGyldighetsperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate fomBruksperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate tomBruksperiode;

    private String landkode;

    public LocalDate getFomGyldighetsperiode() {
        return fomGyldighetsperiode;
    }

    public void setFomGyldighetsperiode(LocalDate fomGyldighetsperiode) {
        this.fomGyldighetsperiode = fomGyldighetsperiode;
    }

    public LocalDate getTomGyldighetsperiode() {
        return tomGyldighetsperiode;
    }

    public void setTomGyldighetsperiode(LocalDate tomGyldighetsperiode) {
        this.tomGyldighetsperiode = tomGyldighetsperiode;
    }

    public LocalDate getFomBruksperiode() {
        return fomBruksperiode;
    }

    public void setFomBruksperiode(LocalDate fomBruksperiode) {
        this.fomBruksperiode = fomBruksperiode;
    }

    public LocalDate getTomBruksperiode() {
        return tomBruksperiode;
    }

    public void setTomBruksperiode(LocalDate tomBruksperiode) {
        this.tomBruksperiode = tomBruksperiode;
    }

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String value) {
        this.landkode = value;
    }

}
