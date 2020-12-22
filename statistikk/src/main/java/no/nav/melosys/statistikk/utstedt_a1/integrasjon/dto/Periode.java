package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;

public class Periode implements ErPeriode {
    private final LocalDate fom;
    private final LocalDate tom;

    public Periode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

    public static Periode av(ErPeriode erPeriode) {
        return new Periode(erPeriode.getFom(), erPeriode.getTom());
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    @Override
    public String toString() {
        return "Periode{" +
            "fom=" + fom +
            ", tom=" + tom +
            '}';
    }
}
