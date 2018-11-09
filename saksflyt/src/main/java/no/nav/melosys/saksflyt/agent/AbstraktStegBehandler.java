package no.nav.melosys.saksflyt.agent;

import java.util.Map;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.*;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.api.StegBehandler;
import no.nav.melosys.saksflyt.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;


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
    @Transactional
    public void utførSteg(Prosessinstans prosessinstans) {
        try {
            utfør(prosessinstans);
        } catch (SikkerhetsbegrensningException e) {
            String feilmelding = "Uventet SikkerhetsbegrensningException";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.INGEN_TILGANG, prosessinstans, feilmelding, e);
        } catch (IkkeFunnetException e) {
            String feilmelding = "Uventet IkkeFunnetException";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.IKKE_FUNNET, prosessinstans, feilmelding, e);
        } catch (FunksjonellException e) {
            String feilmelding = "Uventet FunksjonellException";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, e);
        } catch (TekniskException e) {
            String feilmelding = "Uventet TekniskException";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, e);
        } catch (RuntimeException e) {
            String feilmelding = "Uventet RuntimeException";
            log.error("{}: {}", prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.UVENTET_EXCEPTION, prosessinstans, feilmelding, e);
        }
    }

    protected abstract void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException;

}
