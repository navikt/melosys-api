package no.nav.melosys.domain.datavarehus;

import no.nav.melosys.domain.Behandling;

public class BehandlingLagretEvent {

    public Behandling behandling;

    public BehandlingLagretEvent(Behandling behandling) {
        this.behandling = behandling;
    }
}
