package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public interface StegBehandler {

    ProsessSteg inngangsSteg();

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = MelosysException.class)
    void utfør(Prosessinstans prosessinstans) throws MelosysException;
}
