package no.nav.melosys.service.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.metrics.MetrikkerNavn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class FeiletEventHandler {
    private static final Logger log = LoggerFactory.getLogger(FeiletEventHandler.class);
    private final Counter feiledeEventsCounter = Metrics.counter(MetrikkerNavn.EVENTS_FEILET);

    @Async
    @EventListener
    public void behandleFeiletEvent(FeiletEvent feiletEvent) {
        log.error("Feil ved håndtering av event for behandling {}",
            feiletEvent.getSourceEvent().getBehandlingID(), feiletEvent.getFeil());
        feiledeEventsCounter.increment();
    }
}
