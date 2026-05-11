package no.nav.melosys.saksflyt;

import no.nav.melosys.saksflytapi.domain.Prioritet;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Legger en saksflyt-saga inn på prioritetskøen ({@code saksflytThreadPoolTaskExecutor}).
 *
 * <p>Tidligere ble dette gjort via {@code @Async} på {@link ProsessinstansBehandler#behandleProsessinstansNå};
 * nå pakkes sagaen i en {@link PrioritertProsessinstansOppgave} og sendes via {@code execute(Runnable)} (ikke
 * {@code submit(...)}, som ville pakket den i en ikke-{@code Comparable} {@code FutureTask}), slik at køen kan
 * prioritere HØY/NORMAL foran LAV (batch). Effektiv prioritet hentes fra prosessinstansen
 * ({@link Prosessinstans#hentPrioritet()}).
 */
@Component
public class ProsessinstansDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansDispatcher.class);

    private final ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor;
    private final ProsessinstansBehandler prosessinstansBehandler;

    public ProsessinstansDispatcher(@Qualifier("saksflytThreadPoolTaskExecutor") ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor,
                                    ProsessinstansBehandler prosessinstansBehandler) {
        this.saksflytThreadPoolTaskExecutor = saksflytThreadPoolTaskExecutor;
        this.prosessinstansBehandler = prosessinstansBehandler;
    }

    public void dispatch(Prosessinstans prosessinstans) {
        Prioritet prioritet = prosessinstans.hentPrioritet();
        log.info("Legger prosessinstans {} (prioritet {}) på saksflyt-køen", prosessinstans.getId(), prioritet);
        saksflytThreadPoolTaskExecutor.execute(new PrioritertProsessinstansOppgave(
            prosessinstans.getId(),
            prioritet,
            prosessinstans.getRegistrertDato(),
            () -> prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)
        ));
    }
}
