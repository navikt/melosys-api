package no.nav.melosys.domain.jpa;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerKonverterer;

public class MottatteOpplysningerListener {

    @PrePersist
    public void oppdaterMottatteOpplysninger(MottatteOpplysninger mottatteOpplysninger) {
        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger);
    }

    @PostUpdate
    @PostLoad
    public void lastMottatteOpplysninger(MottatteOpplysninger mottatteOpplysninger) {
        MottatteOpplysningerKonverterer.lastMottatteOpplysninger(mottatteOpplysninger);
    }
}
