package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SaksflytAsyncArbeider {

    private static final Logger log = LoggerFactory.getLogger(SaksflytAsyncArbeider.class);

    private final ProsessinstansBehandler prosessinstansBehandler;

    public SaksflytAsyncArbeider(ProsessinstansBehandler prosessinstansBehandler) {
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    @Async("saksflytThreadPoolTaskExecutor")
    public void behandleProsessinstans(Prosessinstans prosessinstans) {
        try {
            prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        } catch (Exception e) {
            //Evt. Error-behandling, sette prosessinstans til feilet om ikke allerede gjort av ProsessinstansBehandler
            log.error("Uh, oh." , e);
        }
    }
}
