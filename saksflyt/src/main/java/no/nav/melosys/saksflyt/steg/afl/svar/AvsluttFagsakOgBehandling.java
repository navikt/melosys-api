package no.nav.melosys.saksflyt.steg.afl.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AvvisUtpekingAvsluttFagsakOgBehandling")
public class AvsluttFagsakOgBehandling extends SakOgBehandlingStegBehander {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakService fagsakService;

    public AvsluttFagsakOgBehandling(SakOgBehandlingFasade sakOgBehandlingFasade, BehandlingService behandlingService, TpsFasade tpsFasade, FagsakService fagsakService) {
        super(sakOgBehandlingFasade, tpsFasade, behandlingService);
        this.fagsakService = fagsakService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SVAR_AVSLUTT_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.AVSLUTTET);
        sakOgBehandlingAvsluttet(fagsak.getSaksnummer(), behandling.getId(), fagsak.hentBruker().getAktørId());
        log.info("Behandling {} og fagsak {} avsluttet", behandling.getId(), behandling.getFagsak().getSaksnummer());
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
