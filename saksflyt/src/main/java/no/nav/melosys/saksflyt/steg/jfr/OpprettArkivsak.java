package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
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

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_ARKIVSAK;

@Component
public class OpprettArkivsak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettArkivsak.class);

    private final FagsakService fagsakService;
    private final SakService sakService;

    @Autowired
    public OpprettArkivsak(FagsakService fagsakService, SakService sakService) {
        this.fagsakService = fagsakService;
        this.sakService = sakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_ARKIVSAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();

        if (fagsak.getGsakSaksnummer() != null) {
            throw new FunksjonellException("Kan ikke knytte fagsak " + fagsak.getSaksnummer() + " til ny arkivsak: allerede knyttet til " + fagsak.getGsakSaksnummer());
        }

        String aktørId = fagsak.hentBruker().getAktørId();
        String saksnummer = fagsak.getSaksnummer();
        Behandlingstema behandlingstema = behandling.getTema();

        Long gsakSakId = sakService.opprettSak(saksnummer, behandlingstema, aktørId);
        fagsak.setGsakSaksnummer(gsakSakId);
        fagsakService.lagre(fagsak);

        log.info("Opprettet arkivsak {} for fagsak {}", gsakSakId, saksnummer);
    }
}
