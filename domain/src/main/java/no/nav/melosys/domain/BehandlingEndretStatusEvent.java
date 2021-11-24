package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;

public class BehandlingEndretStatusEvent extends BehandlingEvent {

    private final Behandlingsstatus behandlingsstatus;

    public BehandlingEndretStatusEvent(long behandlingID, Behandlingsstatus behandlingsstatus) {
        super(behandlingID);
        this.behandlingsstatus = behandlingsstatus;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }
}
