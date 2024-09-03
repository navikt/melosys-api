package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.avgift.Årsavregning;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING;

@Component
public class AvsluttFagsakOgBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final SaksbehandlingRegler saksbehandlingRegler;


    public AvsluttFagsakOgBehandling(FagsakService fagsakService,
                                     BehandlingService behandlingService,
                                     BehandlingsresultatService behandlingsresultatService,
                                     SaksbehandlingRegler saksbehandlingRegler) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AVSLUTT_SAK_OG_BEHANDLING;
    }

    // se etter medlemskapsperioder og fakturering av trygdeavgift forskuddsvis
    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        Fagsak fagsak = fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer());

        if (behandlingsresultat.erGodkjenningEllerInnvilgelseArt13()
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandlingsresultat.getBehandling())) {
            behandlingService.endreStatus(behandlingID, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        } else if (prosessinstans.getType() == ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING) {
            avsluttÅrsavregning(behandlingsresultat, behandlingID, fagsak, behandling);
        } else {
            avsluttFagsak(prosessinstans, behandlingID, fagsak);
        }
    }

    private void avsluttFagsak(Prosessinstans prosessinstans, long behandlingID, Fagsak fagsak) {
        var saksstatus = prosessinstans.getData(ProsessDataKey.SAKSSTATUS, Saksstatuser.class, Saksstatuser.LOVVALG_AVKLART);
        log.info("Avslutter behandling {}, og setter saksstatus til {} på tilhørende fagsak", behandlingID, saksstatus);
        fagsakService.avsluttFagsakOgBehandling(fagsak, saksstatus);
    }

    private void avsluttÅrsavregning(Behandlingsresultat behandlingsresultat, long behandlingID, Fagsak fagsak, Behandling behandling) {
        Årsavregning årsavregning = behandlingsresultat.getårsavregning();
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.FERDIGBEHANDLET);

        boolean sakLukkes = fagsak.erEnesteBehandling(behandlingID)
            && !årsavregning.erTidligereFakturert()
            && behandlingsresultat.getMedlemskapsperioder().isEmpty();

        if (sakLukkes) {
            fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET);
        } else {
            behandlingService.avsluttBehandling(behandlingID);
        }
    }
}
