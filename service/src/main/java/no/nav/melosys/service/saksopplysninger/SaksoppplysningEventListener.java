package no.nav.melosys.service.saksopplysninger;


import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SaksoppplysningEventListener {

    private final BehandlingService behandlingService;
    private final PersonopplysningerLagrer personopplysningerLagrer;

    public SaksoppplysningEventListener(BehandlingService behandlingService,
                                        PersonopplysningerLagrer personopplysningerLagrer) {
        this.behandlingService = behandlingService;
        this.personopplysningerLagrer = personopplysningerLagrer;
    }

    /**
     * Lagrer personopplysninger ved statusendringer.
     * <p>
     * Merk: IVERKSETTER_VEDTAK håndteres av LagrePersonopplysninger saga-steg for å unngå race condition.
     */
    @EventListener
    @Transactional
    public void lagrePersonopplysninger(BehandlingEndretStatusEvent event) {
        if (List.of(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
            .contains(event.getBehandlingsstatus())) {
            Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(event.getBehandling().getId());
            personopplysningerLagrer.lagreHvisMangler(behandling);
        }
    }
}
