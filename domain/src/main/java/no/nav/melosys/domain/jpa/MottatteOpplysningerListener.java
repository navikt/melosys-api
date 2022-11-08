package no.nav.melosys.domain.jpa;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;

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
