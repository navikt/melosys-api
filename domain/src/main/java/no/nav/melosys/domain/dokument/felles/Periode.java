package no.nav.melosys.domain.dokument.felles;

import java.time.LocalDate;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.jaxb.OffsetDateTimeToLocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Periode implements ErPeriode {

    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate fom;

    @XmlJavaTypeAdapter(OffsetDateTimeToLocalDateXmlAdapter.class)
    private LocalDate tom;

    public Periode () {
    }
    
    public Periode (LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    @Override
    public LocalDate getFom() {
        return fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(fom).append(" → ").append(tom).toString();
    }
    
}
