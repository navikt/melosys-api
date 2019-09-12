package no.nav.melosys.service.kafka.model;

import java.time.LocalDate;

public class Periode {

    private LocalDate fom;

    private LocalDate tom;

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
