package no.nav.melosys.domain.brev;

import java.util.Objects;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public final class Mottaker {
    private Aktoer aktør;

    private Mottaker() {
        super();
    }

    private Mottaker(Aktoer aktør) {
        this.aktør = aktør;
    }

    public static Mottaker av(Aktoer aktør) {
        return new Mottaker(aktør);
    }

    public static Mottaker av(Aktoersroller rolle) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(rolle);
        return new Mottaker(aktør);
    }

    public Aktoer getAktør() {
        return aktør;
    }

    public Aktoersroller hentAktørsRolle() {
        return aktør.getRolle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mottaker)) {
            return false;
        }
        Mottaker mottaker = (Mottaker) o;
        return aktør.equals(mottaker.aktør);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktør);
    }
}
