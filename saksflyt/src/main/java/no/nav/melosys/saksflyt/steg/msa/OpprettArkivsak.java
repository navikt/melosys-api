package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpprettArkivsak implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettArkivsak.class);

    private final SakService sakService;
    private final FagsakService fagsakService;

    public OpprettArkivsak(SakService sakService, FagsakService fagsakService) {
        this.sakService = sakService;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_OPPRETT_ARKIVSAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();
        final Long arkivsakID = sakService.opprettSak(fagsak.getSaksnummer(), behandling.getTema(), fagsak.hentBruker().getAktørId());
        fagsak.setGsakSaksnummer(arkivsakID);
        fagsakService.lagre(fagsak);

        log.info("Opprettet arkivsak {} for {}", arkivsakID, fagsak.getSaksnummer());

        prosessinstans.setSteg(ProsessSteg.MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST);
    }
}
