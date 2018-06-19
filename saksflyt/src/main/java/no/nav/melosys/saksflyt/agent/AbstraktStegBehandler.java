package no.nav.melosys.saksflyt.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.StegBehandler;
import no.nav.melosys.saksflyt.impl.Utils;


public abstract class AbstraktStegBehandler implements StegBehandler {
    
    private Predicate<Prosessinstans> inngangsvilkår;
    
    private Map<Object, UnntakBehandler> unntakBehandlere = new HashMap<>();
    
    public AbstraktStegBehandler() {
        inngangsvilkår = Utils.medSteg(inngangsSteg()).and(Utils.somIkkeSover);
    }

    protected abstract ProsessSteg inngangsSteg();
    
    protected void registrerUnntaksHåndterer(Object unntakGruppe, UnntakBehandler ub) {
        unntakBehandlere.put(unntakGruppe, ub);
    }

    protected void registrerUnntaksHåndtering(Map<Object, UnntakBehandler> ubMap) {
        unntakBehandlere.entrySet().stream().forEach(k -> this.unntakBehandlere.put(k, ubMap.get(k)));
    }

    /**
     * Kalles av arbeidertråder når utførSteg kaster Exception
     */
    protected void håndterUnntak(Object unntak, Prosessinstans prosessinstans, Throwable e) {
        UnntakBehandler ub = unntakBehandlere.get(unntak);
        ub.behandleUnntak(prosessinstans, e); // OK med NPE hvis ub er null
    }
    
    @Override
    public Predicate<Prosessinstans> inngangsvilkår() {
        return inngangsvilkår;
    }

}
