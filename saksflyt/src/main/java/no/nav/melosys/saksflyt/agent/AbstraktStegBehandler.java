package no.nav.melosys.saksflyt.agent;

import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.StegBehandler;
import no.nav.melosys.saksflyt.impl.Utils;


public abstract class AbstraktStegBehandler implements StegBehandler {

    private Predicate<Prosessinstans> inngangsvilkår;
    
    public AbstraktStegBehandler() {
        inngangsvilkår = Utils.medSteg(inngangsSteg()).and(Utils.somIkkeSover);
    }

    protected abstract ProsessSteg inngangsSteg();
    
    protected void registrerUnntaksHåndterer() {
        // FIXME: MELOSYS-1315
    }

    /**
     * Kalles av arbeidertråder når utførSteg kaster Exception
     */
    protected void håndterUnntak(Object unntak, Prosessinstans prosessinstans, Throwable e) {
        // FIXME: MELOSYS-1315
    }
    
    @Override
    public Predicate<Prosessinstans> inngangsvilkår() {
        return inngangsvilkår;
    }

}
