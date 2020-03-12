package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.sak.SakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.STATUS_BEH_OPPR;

/**
 * Oppretter en sak i GSAK.
 *
 * Transisjoner:
 * JFR_OPPRETT_GSAK_SAK -> STATUS_BEH_OPPR eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettSak extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettSak.class);

    private final FagsakRepository fagsakRepository;
    private final SakService sakService;

    @Autowired
    public OpprettSak(FagsakRepository fagsakRepository, SakService sakService) {
        this.fagsakRepository = fagsakRepository;
        this.sakService = sakService;
        log.info("OpprettGsakSak initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_GSAK_SAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak.getGsakSaksnummer() != null) {
            String feilmelding = "Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny sak: allerede knyttet til " + fagsak.getGsakSaksnummer();
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Long gsakSakId = sakService.opprettSak(saksnummer, behandlingstype, aktørId);
        fagsak.setGsakSaksnummer(gsakSakId);
        fagsakRepository.save(fagsak);
        prosessinstans.setData(GSAK_SAK_ID, gsakSakId);

        prosessinstans.setSteg(STATUS_BEH_OPPR);
        log.info("Prosessinstans {} opprettet NAV-sak {} for fagsak {}", prosessinstans.getId(), gsakSakId, saksnummer);
    }
}
