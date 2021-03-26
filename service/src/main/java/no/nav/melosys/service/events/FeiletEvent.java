package no.nav.melosys.service.events;

import no.nav.melosys.domain.behandling.BehandlingEvent;
import org.springframework.context.ApplicationEvent;

public class FeiletEvent extends ApplicationEvent {
    private final Exception feil;
    private final BehandlingEvent sourceEvent;

    public FeiletEvent(Object source, Exception feil, BehandlingEvent sourceEvent) {
        super(source);
        this.feil = feil;
        this.sourceEvent = sourceEvent;
    }

    public Exception getFeil() {
        return feil;
    }

    public BehandlingEvent getSourceEvent() {
        return sourceEvent;
    }
}
