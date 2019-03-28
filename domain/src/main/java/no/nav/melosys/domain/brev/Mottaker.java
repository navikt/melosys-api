package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public final class Mottaker {
    private final Aktoersroller rolle;

    public Mottaker(Aktoersroller rolle) {
        this.rolle = rolle;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }
}
