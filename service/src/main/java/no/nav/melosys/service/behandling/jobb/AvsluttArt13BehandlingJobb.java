package no.nav.melosys.service.behandling.jobb;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AvsluttArt13BehandlingJobb {

    private static final Logger log = LoggerFactory.getLogger(AvsluttArt13BehandlingJobb.class);

    private final BehandlingService behandlingService;
    private final AvsluttArt13BehandlingService avsluttArt13BehandlingService;

    public AvsluttArt13BehandlingJobb(BehandlingService behandlingService, AvsluttArt13BehandlingService avsluttArt13BehandlingService) {
        this.behandlingService = behandlingService;
        this.avsluttArt13BehandlingService = avsluttArt13BehandlingService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void avsluttBehandlingArt13() {
        behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
            .stream()
            .map(Behandling::getId)
            .forEach(this::avsluttHvis2MndHarPassert);
    }

    private void avsluttHvis2MndHarPassert(long behandlingID) {
        try {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        } catch (Exception e) {
            log.error("Feil ved avslutting av behandling {}", behandlingID, e);
        }
    }
}
