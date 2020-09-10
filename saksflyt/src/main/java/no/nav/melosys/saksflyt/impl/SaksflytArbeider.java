package no.nav.melosys.saksflyt.impl;

import java.util.Optional;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.saksflyt.api.ProsessinstansKø;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class SaksflytArbeider implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SaksflytArbeider.class);
    private static final long SOV_MELLOM_OPPGAVER = 200;

    private final ProsessinstansKø binge;
    private final ProsessinstansBehandler prosessinstansBehandler;

    public SaksflytArbeider(ProsessinstansKø binge, ProsessinstansBehandler prosessinstansBehandler) {
        this.binge = binge;
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    @SuppressWarnings("java:S2189")
    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            Optional<Prosessinstans> plukketProsessinstans = binge.plukkNeste();
            try {
                plukketProsessinstans.ifPresent(prosessinstansBehandler::behandleProsessinstans);
                Thread.sleep(SOV_MELLOM_OPPGAVER);
            } catch (InterruptedException e) {
                log.error("Arbeidertråd avbrutt", e);
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                String feilmelding = plukketProsessinstans.map(p -> "Plukket prosessinstans " + p.getId()).orElse("");
                log.error("Ubehandlet exception. {}", feilmelding, e);
            }
        }
    }
}
