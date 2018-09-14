package no.nav.melosys.service.datavarehus;

import no.nav.melosys.domain.datavarehus.BehandlingDvh;
import no.nav.melosys.domain.datavarehus.BehandlingLagretEvent;
import no.nav.melosys.domain.datavarehus.FagsakDvh;
import no.nav.melosys.domain.datavarehus.FagsakLagretEvent;
import no.nav.melosys.repository.BehandlingDvhRepository;
import no.nav.melosys.repository.FagsakDvhRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatavarehusEventObserver {

    private static final Logger log = LoggerFactory.getLogger(DatavarehusEventObserver.class);

    private final FagsakDvhRepository fagsakDvhRepository;

    private final BehandlingDvhRepository behandlingDvhRepository;

    public DatavarehusEventObserver(FagsakDvhRepository fagsakDvhRepository, BehandlingDvhRepository behandlingDvhRepository) {
        this.fagsakDvhRepository = fagsakDvhRepository;
        this.behandlingDvhRepository = behandlingDvhRepository;
    }

    @EventListener(FagsakLagretEvent.class)
    public void håndterFagsakLagretEvent(FagsakLagretEvent fagsakLagretEvent) {
        FagsakDvh fagsakDvh = new FagsakDvh();
        fagsakDvh.setSaksnummer(fagsakLagretEvent.fagsak.getSaksnummer());
        fagsakDvhRepository.save(fagsakDvh);
        log.info("Fagsak med saksnummer " + fagsakLagretEvent.fagsak.getSaksnummer() + " lagret");
    }

    @EventListener(BehandlingLagretEvent.class)
    public void håndterBehandlingLagretEvent(BehandlingLagretEvent behandlingLagretEvent) {
        BehandlingDvh behandlingDvh = new BehandlingDvh();
        behandlingDvh.setBehandlingId(behandlingLagretEvent.behandling.getId());
        behandlingDvhRepository.save(behandlingDvh);
        log.info("Behandling med behandlingId " + behandlingLagretEvent.behandling.getId() + " lagret");
    }

}
