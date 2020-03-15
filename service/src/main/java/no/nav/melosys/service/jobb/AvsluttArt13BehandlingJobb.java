package no.nav.melosys.service.jobb;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static no.nav.melosys.service.kontroll.PeriodeKontroller.datoEldreEnn2Mnd;

@Component
public class AvsluttArt13BehandlingJobb {

    private static final Logger log = LoggerFactory.getLogger(AvsluttArt13BehandlingJobb.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AvsluttArt13BehandlingService avsluttArt13BehandlingService;

    public AvsluttArt13BehandlingJobb(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, AvsluttArt13BehandlingService avsluttArt13BehandlingService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.avsluttArt13BehandlingService = avsluttArt13BehandlingService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void avsluttBehandlingArt13() {
        behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
            .forEach(this::avsluttHvis2MndHarPassert);
    }

    private void avsluttHvis2MndHarPassert(Behandling behandling) {
        try {
            if (toMndHarPassertSidenSaksbehandling(behandling)) {
                log.info("Forsøker å avslutte behandling {}", behandling.getId());
                avsluttArt13BehandlingService.avsluttBehandling(behandling.getId());
            }
        } catch (Exception e) {
          log.error("Feil ved avslutting av behandling {}", behandling.getId(), e);
        }
    }

    private boolean toMndHarPassertSidenSaksbehandling(Behandling behandling) throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        if (!behandlingsresultat.hentValidertLovvalgsperiode().erArtikkel13()) {
            throw new FunksjonellException("Behandling skal ikke avsluttes automatisk da den er av bestemmelse"
                + behandlingsresultat.hentValidertLovvalgsperiode().getBestemmelse());
        }
        return behandling.kanResultereIVedtak()
            ? datoEldreEnn2Mnd(behandlingsresultat.getVedtakMetadata().getVedtaksdato())
            : datoEldreEnn2Mnd(behandlingsresultat.getEndretDato());
    }
}
