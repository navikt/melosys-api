package no.nav.melosys.saksflyt.steg.afl.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sob.SobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AvvisUtpekingAvsluttFagsakOgBehandling")
public class AvsluttFagsakOgBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final SobService sobService;

    @Autowired
    public AvsluttFagsakOgBehandling(FagsakService fagsakService, SobService sobService) {
        this.fagsakService = fagsakService;
        this.sobService = sobService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SVAR_AVSLUTT_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.AVSLUTTET);
        sobService.sakOgBehandlingAvsluttet(fagsak.getSaksnummer(), behandling.getId(), fagsak.hentBruker().getAktørId());
        log.info("Behandling {} og fagsak {} avsluttet", behandling.getId(), behandling.getFagsak().getSaksnummer());
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
