package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.datavarehus.FagsakLagretEvent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessSteg.STATUS_BEH_OPPR;

/**
 * Oppretter en sak i GSAK.
 *
 * Transisjoner:
 * JFR_OPPRETT_GSAK_SAK -> STATUS_BEH_OPPR eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettGsakSak extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettGsakSak.class);

    private final FagsakRepository fagsakRepository;
    private final GsakFasade gsakFasade;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OpprettGsakSak(@Qualifier("system")GsakFasade gsakFasade, FagsakRepository fagsakRepository,
                          ApplicationEventPublisher applicationEventPublisher) {
        this.fagsakRepository = fagsakRepository;
        this.gsakFasade = gsakFasade;
        this.applicationEventPublisher = applicationEventPublisher;
        log.info("OpprettGsakSak initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_GSAK_SAK;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, IntegrasjonException, SikkerhetsbegrensningException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak.getGsakSaksnummer() != null) {
            String feilmelding = "Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny GSAK sak: allerede knyttet til " + fagsak.getGsakSaksnummer();
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Long gsakSakId = gsakFasade.opprettSak(saksnummer, Behandlingstype.SØKNAD, aktørId);
        fagsak.setGsakSaksnummer(gsakSakId);
        fagsakRepository.save(fagsak);
        applicationEventPublisher.publishEvent(new FagsakLagretEvent(fagsak));
        prosessinstans.setData(GSAK_SAK_ID, gsakSakId);

        prosessinstans.setSteg(STATUS_BEH_OPPR);
        log.info("Prosessinstans {} opprettet GSAK sak {} for fagsak {}", prosessinstans.getId(), gsakSakId, saksnummer);
    }
}
