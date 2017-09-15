package no.nav.melosys.domain.dokument.felles;

import java.time.LocalDate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Periode {

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate fom;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate tom;
}
