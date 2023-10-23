package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public interface StegBehandler {

    ProsessSteg inngangsSteg();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void utfør(Prosessinstans prosessinstans);
}
