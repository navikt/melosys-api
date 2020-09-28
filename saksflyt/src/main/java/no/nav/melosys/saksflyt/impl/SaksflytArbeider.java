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
    private static final long PAUSE_MELLOM_OPPGAVER_MS = 200;

    private final ProsessinstansKø prosessinstansKø;
    private final ProsessinstansBehandler prosessinstansBehandler;

    public SaksflytArbeider(ProsessinstansKø prosessinstansKø, ProsessinstansBehandler prosessinstansBehandler) {
        this.prosessinstansKø = prosessinstansKø;
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    @SuppressWarnings({"java:S2189", "BusyWait"})
    @Override
    public void run() {
        while (true) {
            Optional<Prosessinstans> plukketProsessinstans = prosessinstansKø.plukkNeste();
            try {
                plukketProsessinstans.ifPresent(prosessinstansBehandler::behandleProsessinstans);
                Thread.sleep(PAUSE_MELLOM_OPPGAVER_MS);
            } catch (InterruptedException e) {
                log.warn("Arbeidertråd avbrutt", e);
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                String feilmelding = plukketProsessinstans.map(p -> "Plukket prosessinstans " + p.getId()).orElse("");
                log.error("Ubehandlet exception. {}", feilmelding, e);
            }
        }
    }
}
