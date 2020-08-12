package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
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
public class OpprettSak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettSak.class);

    private final FagsakService fagsakService;
    private final SakService sakService;

    @Autowired
    public OpprettSak(FagsakService fagsakService, SakService sakService) {
        this.fagsakService = fagsakService;
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
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        if (fagsak.getGsakSaksnummer() != null) {
            throw new FunksjonellException("Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny sak: allerede knyttet til " + fagsak.getGsakSaksnummer());
        }

        Long gsakSakId = sakService.opprettSak(saksnummer, behandlingstema, aktørId);
        fagsak.setGsakSaksnummer(gsakSakId);
        fagsakService.lagre(fagsak);
        prosessinstans.setData(GSAK_SAK_ID, gsakSakId);

        prosessinstans.setSteg(STATUS_BEH_OPPR);
        log.info("Prosessinstans {} opprettet NAV-sak {} for fagsak {}", prosessinstans.getId(), gsakSakId, saksnummer);
    }
}
