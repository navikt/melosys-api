package no.nav.melosys.domain.brev;

import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public final class Mottaker {
    private final Aktoersroller rolle;

    private Mottaker(Aktoersroller rolle) {
        this.rolle = rolle;
    }

    public static Mottaker av(Aktoersroller rolle) {
        return new Mottaker(rolle);
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mottaker)) return false;
        Mottaker mottaker = (Mottaker) o;
        return getRolle() == mottaker.getRolle();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRolle());
    }
}
