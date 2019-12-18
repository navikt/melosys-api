package no.nav.melosys.saksflyt.steg.gsak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class OpprettGsakSak extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettGsakSak.class);

    private final FagsakRepository fagsakRepository;
    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettGsakSak(@Qualifier("system")GsakFasade gsakFasade, FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
        this.gsakFasade = gsakFasade;
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
            String feilmelding = "Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny GSAK sak: allerede knyttet til " + fagsak.getGsakSaksnummer();
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Long gsakSakId = gsakFasade.opprettSak(saksnummer, behandlingstype, aktørId);
        fagsak.setGsakSaksnummer(gsakSakId);
        fagsakRepository.save(fagsak);
        prosessinstans.setData(GSAK_SAK_ID, gsakSakId);

        prosessinstans.setSteg(STATUS_BEH_OPPR);
        log.info("Prosessinstans {} opprettet GSAK sak {} for fagsak {}", prosessinstans.getId(), gsakSakId, saksnummer);
    }
}
