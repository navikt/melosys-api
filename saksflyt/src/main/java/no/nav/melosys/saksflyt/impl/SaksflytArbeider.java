package no.nav.melosys.saksflyt.impl;

import java.util.Optional;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
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
    private final ProsessinstansRepository prosessinstansRepository;

    public SaksflytArbeider(ProsessinstansKø prosessinstansKø, ProsessinstansBehandler prosessinstansBehandler, ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansKø = prosessinstansKø;
        this.prosessinstansBehandler = prosessinstansBehandler;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @SuppressWarnings({"java:S2189", "java:S1181", "BusyWait"})
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
            } catch (Throwable t) {
                log.error("Ubehandlet exception. {}", plukketProsessinstans.map(p -> "Plukket prosessinstans " + p.getId()).orElse(""), t);

                plukketProsessinstans.ifPresent(p -> {
                    p.setStatus(ProsessStatus.FEILET);
                    prosessinstansRepository.save(p);
                });

                if (erKritisk(t)) {
                    log.error("{} er markert som en kritisk feil. Stopper SaksflytArbeider", t.getClass().getSimpleName());
                    break;
                }
            }
        }
    }

    private boolean erKritisk(Throwable t) {
        return t instanceof VirtualMachineError
            || t instanceof ThreadDeath
            || t instanceof LinkageError;
    }
}
