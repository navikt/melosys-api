package no.nav.melosys.service.saksopplysninger;


import java.util.List;

import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SaksoppplysningEventListener {

    private final PersonopplysningerLagrer personopplysningerLagrer;

    public SaksoppplysningEventListener(PersonopplysningerLagrer personopplysningerLagrer) {
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
            personopplysningerLagrer.lagreHvisMangler(event.getBehandling().getId());
        }
    }
}
