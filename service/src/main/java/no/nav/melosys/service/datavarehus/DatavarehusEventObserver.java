package no.nav.melosys.service.datavarehus;

import no.nav.melosys.domain.BehandlingLagretEvent;
import no.nav.melosys.domain.FagsakLagretEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatavarehusEventObserver {

    private static final Logger log = LoggerFactory.getLogger(DatavarehusEventObserver.class);

    // FIXME lagre til datavarehustabeller hvis relevant endring

    @EventListener(FagsakLagretEvent.class)
    public void håndterFagsakLagretEvent(FagsakLagretEvent fagsakLagretEvent) {
        log.info("Fagsak med saksnummer " + fagsakLagretEvent.fagsak.getSaksnummer() + " lagret");
    }

    @EventListener(BehandlingLagretEvent.class)
    public void håndterBehandlingLagretEvent(BehandlingLagretEvent behandlingLagretEvent) {
        log.info("Behandling med behandlingId " + behandlingLagretEvent.behandling.getId() + " lagret");
    }

}
