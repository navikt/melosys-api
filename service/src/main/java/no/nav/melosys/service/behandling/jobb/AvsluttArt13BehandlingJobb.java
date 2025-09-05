package no.nav.melosys.service.behandling.jobb;

import java.util.Collection;
import java.util.UUID;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
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
    @SchedulerLock(name = "avsluttBehandlingArt13Jobb", lockAtLeastFor = "10m")
    public void avsluttBehandlingArt13() {
        UUID processId = UUID.randomUUID();
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, "AvsluttArt13BehandlingJobb");
        try {
            log.info("Starter avsluttBehandlingArt13Jobb");
            Collection<Behandling> behandlinger = behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
            log.info("Antall behandlinger med status MIDLERTIDIG_LOVVALGSBESLUTNING: {}", behandlinger.size());
            behandlinger
                .stream()
                .map(Behandling::getId)
                .forEach(this::avsluttHvis2MndHarPassert);
        } catch (Exception e) {
            log.error("Feil ved kjøring av avsluttBehandlingArt13Jobb", e);
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId);
        }
    }

    private void avsluttHvis2MndHarPassert(long behandlingID) {
        try {
            log.info("Starter avsluttBehandlingArt13Jobb for behandling {}", behandlingID);
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID);
        } catch (Exception e) {
            log.error("Feil ved avslutting av behandling {}", behandlingID, e);
        }
    }
}
