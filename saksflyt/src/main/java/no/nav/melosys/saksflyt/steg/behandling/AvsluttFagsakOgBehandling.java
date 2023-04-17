package no.nav.melosys.saksflyt.steg.behandling;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING;

@Component
public class AvsluttFagsakOgBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    private final Unleash unleash;


    public AvsluttFagsakOgBehandling(FagsakService fagsakService,
                                     BehandlingService behandlingService,
                                     BehandlingsresultatService behandlingsresultatService,
                                     Unleash unleash) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AVSLUTT_SAK_OG_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        var registreringUnntakToggleEnabled = unleash.isEnabled(ToggleName.REGISTRERING_UNNTAK_FRA_MEDLEMSKAP);

        if (behandlingsresultat.erGodkjenningEllerInnvilgelseArt13()
            && !SaksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandlingsresultat.getBehandling(), registreringUnntakToggleEnabled)) {
            behandlingService.endreStatus(behandlingID, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        } else {
            var saksstatus = prosessinstans.getData(ProsessDataKey.SAKSSTATUS, Saksstatuser.class, Saksstatuser.LOVVALG_AVKLART);
            log.info("Avslutter behandling {}, og setter saksstatus til {} på tilhørende fagsak", behandlingID, saksstatus);
            fagsakService.avsluttFagsakOgBehandling(
                fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer()), saksstatus
            );
        }
    }
}
