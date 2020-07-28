package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVSLUTT_BEHANDLING;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_STATUS_BEH_AVSL;

/**
 * Avslutter en fagsak og Behandling i Melosys.
 *
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_AVSLUTT_BEHANDLING -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvsluttFagsakOgBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public AvsluttFagsakOgBehandling(FagsakService fagsakService, BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_AVSLUTT_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingsresultat behandlingsresultat = behandlingsresultatService
            .hentBehandlingsresultat(prosessinstans.getBehandling().getId());

        if (behandlingsresultat.erInnvilgelse() && behandlingsresultat.hentValidertLovvalgsperiode().erArtikkel13()
            || behandlingsresultat.erUtpeking()) {
            long behandlingID = prosessinstans.getBehandling().getId();
            behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        } else {
            fagsakService.avsluttFagsakOgBehandling(
                fagsakService.hentFagsak(prosessinstans.getBehandling().getFagsak().getSaksnummer()), Saksstatuser.LOVVALG_AVKLART
            );
        }

        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
