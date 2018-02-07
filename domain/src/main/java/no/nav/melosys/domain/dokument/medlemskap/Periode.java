package no.nav.melosys.domain.dokument.medlemskap;

import java.time.LocalDate;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

@XmlType(name = "Periode", namespace = "medlemskaps") // Løser navnekonflikt med no.nav.melosys.domain.dokument.felles.Periode
public class Periode implements ErPeriode {

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate fom;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
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
