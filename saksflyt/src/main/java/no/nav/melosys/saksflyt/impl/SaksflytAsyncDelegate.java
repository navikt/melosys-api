package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SaksflytAsyncDelegate {

    private static final Logger log = LoggerFactory.getLogger(SaksflytAsyncDelegate.class);

    private final ProsessinstansBehandler prosessinstansBehandler;

    public SaksflytAsyncDelegate(ProsessinstansBehandler prosessinstansBehandler) {
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    @SuppressWarnings("java:S1181")
    @Async("saksflytThreadPoolTaskExecutor")
    public void behandleProsessinstans(Prosessinstans prosessinstans) {
        try {
            prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        } catch (Throwable e) {
            log.error("Uventet feil ved behandling av prosessinstans {}", prosessinstans.getId(), e);
        }
    }
}
