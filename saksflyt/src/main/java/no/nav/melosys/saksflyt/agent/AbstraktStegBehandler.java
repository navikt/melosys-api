package no.nav.melosys.saksflyt.agent;

import java.util.Map;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.api.StegBehandler;
import no.nav.melosys.saksflyt.impl.Utils;


public abstract class AbstraktStegBehandler implements StegBehandler {
    
    private Predicate<Prosessinstans> inngangsvilkår;
    
    private Map<Feilkategori, UnntakBehandler> unntakBehandlere;
    
    protected abstract ProsessSteg inngangsSteg();
    
    /**
     * Returnerer en Map som definerer unntakshåndtering for steget. 
     */
    protected abstract Map<Feilkategori, UnntakBehandler> unntaksHåndtering();
    
    public AbstraktStegBehandler() {
        inngangsvilkår = Utils.medSteg(inngangsSteg()).and(Utils.somIkkeSover);
        unntakBehandlere = unntaksHåndtering();
    }

    /**
     * Kalles av subklasser for å håndtere evt. feilsituasjoner.
     */
    protected void håndterUnntak(Feilkategori kategori, Prosessinstans prosessinstans, String melding, Throwable e) {
        UnntakBehandler ub = unntakBehandlere.get(kategori);
        ub.behandleUnntak(prosessinstans, melding, e); // OK med NPE hvis ub er null
    }
    
    @Override
    public Predicate<Prosessinstans> inngangsvilkår() {
        return inngangsvilkår;
    }

}
