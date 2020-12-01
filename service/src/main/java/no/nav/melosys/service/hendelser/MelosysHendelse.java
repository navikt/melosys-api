package no.nav.melosys.service.hendelser;

import org.springframework.context.ApplicationEvent;

public abstract class MelosysHendelse extends ApplicationEvent {
    private final Long behandlingID;

    public MelosysHendelse(Object source, Long behandlingID) {
        super(source);
        this.behandlingID = behandlingID;
    }

    public Long getBehandlingID() {
        return behandlingID;
    }
}
