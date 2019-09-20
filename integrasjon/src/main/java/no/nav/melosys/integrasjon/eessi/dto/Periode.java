package no.nav.melosys.integrasjon.eessi.dto;

import java.time.LocalDate;

public class Periode {

    private LocalDate fom;

    private LocalDate tom;

    public Periode(LocalDate fom, LocalDate tom) {
        this.fom = fom;
        this.tom = tom;
    }

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
}
