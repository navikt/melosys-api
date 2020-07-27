package no.nav.melosys.saksflyt.steg;

import java.util.function.Predicate;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.impl.Utils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public interface StegBehandler {

    default Predicate<Prosessinstans> inngangsvilkår() {
        return Utils.medSteg(inngangsSteg()).and(Utils.somIkkeSover);
    }

    ProsessSteg inngangsSteg();

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = MelosysException.class)
    void utfør(Prosessinstans prosessinstans) throws MelosysException;
}
