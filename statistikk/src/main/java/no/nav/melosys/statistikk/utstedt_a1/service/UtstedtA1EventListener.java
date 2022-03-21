package no.nav.melosys.statistikk.utstedt_a1.service;

import no.nav.melosys.domain.VedtakMetadataLagretEvent;
import no.nav.melosys.service.events.FeiletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UtstedtA1EventListener {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1EventListener.class);

    private final UtstedtA1Service utstedtA1Service;
    private final ApplicationEventMulticaster melosysEventMulticaster;

    public UtstedtA1EventListener(UtstedtA1Service utstedtA1Service, ApplicationEventMulticaster melosysEventMulticaster) {
        this.utstedtA1Service = utstedtA1Service;
        this.melosysEventMulticaster = melosysEventMulticaster;
    }

    @Async
    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void handterA1Bestilt(VedtakMetadataLagretEvent vedtakMetadataLagretEvent) {
        try {
            log.info("Mottatt hendelse om vedtak metadata lagret");
            utstedtA1Service.sendMeldingOmUtstedtA1(vedtakMetadataLagretEvent.getBehandlingID());
        } catch (Exception e) {
            melosysEventMulticaster.multicastEvent(new FeiletEvent(this, e, vedtakMetadataLagretEvent));
        }
    }
}
