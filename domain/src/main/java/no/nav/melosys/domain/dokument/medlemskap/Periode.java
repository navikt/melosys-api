package no.nav.melosys.domain.dokument.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.MuligPeriode;

public class Periode implements ErPeriode, MuligPeriode {

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

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    @Override
    public boolean erGyldig() {
        // From ErPeriode: checks if period includes today
        return this.inkluderer(java.time.LocalDate.now());
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
