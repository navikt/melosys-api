package no.nav.melosys.domain.mottatteopplysninger.data;

import java.time.LocalDate;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.ErPeriode;

@XmlType(name = "Periode", namespace = "soeknad") // Løser navnekonflikt med no.nav.melosys.domain.dokument.felles.Periode
public class Periode implements ErPeriode {

    private LocalDate fom;

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
