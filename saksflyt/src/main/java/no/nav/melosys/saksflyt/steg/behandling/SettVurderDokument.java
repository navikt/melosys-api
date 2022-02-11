package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SettVurderDokument implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SettVurderDokument.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    @Autowired
    public SettVurderDokument(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_SETT_VURDER_DOKUMENT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = fagsak.hentAktivBehandling();
        boolean ingenVurdering = prosessinstans.getData(ProsessDataKey.JFR_INGEN_VURDERING, Boolean.class);
        if (behandling != null && !ingenVurdering) {
            behandlingService.endreStatus(behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            log.info("Endret status på behandling {} til {}", behandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
        } else {
            log.info("Nytt dokument krever ingen vurdering ({}) eller ingen aktiv behandling for sak {}.", ingenVurdering, saksnummer);
        }
    }
}

