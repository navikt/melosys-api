package no.nav.melosys.saksflyt.agent;

import java.util.Map;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.api.StegBehandler;
import no.nav.melosys.saksflyt.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstraktStegBehandler implements StegBehandler {
    
    private static final Logger log = LoggerFactory.getLogger(AbstraktStegBehandler.class);
    
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

    @Override
    public final void utførSteg(Prosessinstans prosessinstans) {
        try {
            utfør(prosessinstans);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Uventet SikkerhetsbegrensningException for {}", prosessinstans.getId(), e);
            håndterUnntak(Feilkategori.INGEN_TILGANG, prosessinstans, "Uventet SikkerhetsbegrensningException", e);
        } catch (IkkeFunnetException e) {
            String feilmelding = "Fant ikke orginfo for en eller flere organisasjoner";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.IKKE_FUNNET, prosessinstans, feilmelding, e);
        } catch (RuntimeException e) {
            log.error("Uventet RuntimeException for {}", prosessinstans.getId(), e);
            håndterUnntak(Feilkategori.UVENTET_EXCEPTION, prosessinstans, "Uventet RuntimeException", e);
        }
    }
    
    protected abstract void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, IkkeFunnetException;

}
