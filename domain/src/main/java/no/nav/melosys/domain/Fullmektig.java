package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Representerer;

public class Fullmektig {
    private final String representantID;
    private final Representerer representerer;

    public Fullmektig(String representantID, Representerer representerer) {
        this.representantID = representantID;
        this.representerer = representerer;
    }

    public String getRepresentantID() {
        return representantID;
    }

    public Representerer getRepresenterer() {
        return representerer;
    }
}
