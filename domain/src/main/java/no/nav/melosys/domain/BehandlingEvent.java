package no.nav.melosys.domain;

import org.springframework.context.ApplicationEvent;

public abstract class BehandlingEvent extends ApplicationEvent {
    private final long behandlingID;

    public BehandlingEvent(long behandlingID) {
        super(behandlingID);
        this.behandlingID = behandlingID;
    }

    public long getBehandlingID() {
        return behandlingID;
    }
}
