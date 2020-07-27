package no.nav.melosys.saksflyt.steg;

import java.util.function.Predicate;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.impl.Utils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public abstract class AbstraktStegBehandler {

    private final Predicate<Prosessinstans> inngangsvilkår;

    public AbstraktStegBehandler() {
        inngangsvilkår = Utils.medSteg(inngangsSteg()).and(Utils.somIkkeSover);
    }

    public Predicate<Prosessinstans> inngangsvilkår() {
        return inngangsvilkår;
    }

    protected abstract ProsessSteg inngangsSteg();

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = MelosysException.class)
    public abstract void utfør(Prosessinstans prosessinstans) throws MelosysException;

}
