package no.nav.melosys.domain.datavarehus;

import no.nav.melosys.domain.Behandling;

public class BehandlingOpprettetEvent {

    public Behandling behandling;

    public String endretAv;

    public BehandlingOpprettetEvent(Behandling behandling, String endretAv) {
        this.behandling = behandling;
        this.endretAv = endretAv;
    }
}
