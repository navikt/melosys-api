package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;

public class BehandlingEndretStatusEvent extends BehandlingEvent {

    private final Behandlingsstatus behandlingsstatus;
    private final Behandling behandling;

    public BehandlingEndretStatusEvent(long behandlingID, Behandlingsstatus behandlingsstatus, Behandling behandling) {
        super(behandlingID);
        this.behandlingsstatus = behandlingsstatus;
        this.behandling = behandling;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public Behandling getBehandling() {
        return behandling;
    }
}
