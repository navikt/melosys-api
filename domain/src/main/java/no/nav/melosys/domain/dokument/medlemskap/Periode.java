package no.nav.melosys.domain.dokument.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Periode)) return false;
        Periode periode = (Periode) o;
        return getFom().equals(periode.getFom()) &&
            Objects.equals(getTom(), periode.getTom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFom(), getTom());
    }
}
