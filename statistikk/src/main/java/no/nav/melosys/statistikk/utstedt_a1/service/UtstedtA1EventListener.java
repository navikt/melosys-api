package no.nav.melosys.statistikk.utstedt_a1.service;

import no.nav.melosys.service.hendelser.FeiletHendelse;
import no.nav.melosys.service.hendelser.VedtakMetadataLagretHendelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UtstedtA1EventListener {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1EventListener.class);

    private final UtstedtA1Service utstedtA1Service;
    private final ApplicationEventMulticaster melosysHendelseMulticaster;

    @Autowired
    public UtstedtA1EventListener(UtstedtA1Service utstedtA1Service, ApplicationEventMulticaster melosysHendelseMulticaster) {
        this.utstedtA1Service = utstedtA1Service;
        this.melosysHendelseMulticaster = melosysHendelseMulticaster;
    }

    @Async
    @TransactionalEventListener
    @SuppressWarnings("unused")
    public void handterA1Bestilt(VedtakMetadataLagretHendelse vedtakMetadataLagretHendelse) {
        try {
            log.info("Mottatt hendelse om vedtak metadata lagret");
            utstedtA1Service.sendMeldingOmUtstedtA1(vedtakMetadataLagretHendelse.getBehandlingID());
        } catch (Exception e) {
            melosysHendelseMulticaster.multicastEvent(new FeiletHendelse(this, e, vedtakMetadataLagretHendelse));
        }
    }
}
