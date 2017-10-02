package no.nav.melosys.domain.dokument.organisasjon;


import java.time.LocalDate;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.OffsetDateTimeToLocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Organisasjonsnavn {

    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    private List<String> navn;

    protected String redigertNavn;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    protected LocalDate fomBruksperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    protected LocalDate tomBruksperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    protected LocalDate fomGyldighetsperiode;

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    protected LocalDate tomGyldighetsperiode;

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

    public LocalDate getFomBruksperiode() {
        return fomBruksperiode;
    }

    public void setFomBruksperiode(LocalDate value) {
        this.fomBruksperiode = value;
    }

    public LocalDate getTomBruksperiode() {
        return tomBruksperiode;
    }

    public void setTomBruksperiode(LocalDate value) {
        this.tomBruksperiode = value;
    }

    public LocalDate getFomGyldighetsperiode() {
        return fomGyldighetsperiode;
    }

    public void setFomGyldighetsperiode(LocalDate value) {
        this.fomGyldighetsperiode = value;
    }

    public LocalDate getTomGyldighetsperiode() {
        return tomGyldighetsperiode;
    }

    public void setTomGyldighetsperiode(LocalDate value) {
        this.tomGyldighetsperiode = value;
    }

}
