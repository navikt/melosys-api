package no.nav.melosys.domain.eessi;

import java.time.LocalDate;
import java.util.Objects;

public class Periode {
    private LocalDate fom;
    private LocalDate tom;

    public Periode() {
    }

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
