package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

public class Periode implements ErPeriode {

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate fom;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate tom;

    public Periode () {
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
