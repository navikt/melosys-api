package no.nav.melosys.domain.dokument.felles;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;

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
