package no.nav.melosys.service.hendelser;

import org.springframework.context.ApplicationEvent;

public class FeiletHendelse extends ApplicationEvent {
    private final Exception feil;
    private final MelosysHendelse sourceEvent;

    public FeiletHendelse(Object source, Exception feil, MelosysHendelse sourceEvent) {
        super(source);
        this.feil = feil;
        this.sourceEvent = sourceEvent;
    }

    public Exception getFeil() {
        return feil;
    }

    public MelosysHendelse getSourceEvent() {
        return sourceEvent;
    }
}
