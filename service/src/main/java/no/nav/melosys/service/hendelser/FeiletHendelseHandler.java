package no.nav.melosys.service.hendelser;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.metrics.MetrikkerNavn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class FeiletHendelseHandler {
    private static final Logger log = LoggerFactory.getLogger(FeiletHendelseHandler.class);
    private final Counter feiledeHendelserCounter = Metrics.counter(MetrikkerNavn.HENDELSER_FEILET);

    @Async
    @EventListener
    public void behandleFeiletHendelse(FeiletHendelse feiletHendelse) {
        log.error("Feil ved håndtering av hendelse for behandling {}",
            feiletHendelse.getSourceEvent().getBehandlingID(), feiletHendelse.getFeil());
        feiledeHendelserCounter.increment();
    }
}
