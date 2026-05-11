package no.nav.melosys.saksflyt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link ThreadPoolTaskExecutor} som bruker en {@link PriorityBlockingQueue} i stedet for default FIFO-køen,
 * slik at oppgaver kan plukkes i prioritert rekkefølge (se {@link PrioritertProsessinstansOppgave}).
 *
 * <p>Bønne-typen forblir {@code ThreadPoolTaskExecutor} (ikke en bar {@link java.util.concurrent.ThreadPoolExecutor}),
 * fordi {@code ProsessinstansService}, {@code SaksflytHealthIndicator}, {@code E2ESupportController} m.fl. injiserer
 * den typen og kaller {@code getThreadPoolExecutor().getQueue()}.
 */
public class PrioritertSaksflytTaskExecutor extends ThreadPoolTaskExecutor {

    private static final int INITIELL_KØKAPASITET = 16;

    @Override
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        // queueCapacity (default Integer.MAX_VALUE) ignoreres bevisst: for en PriorityBlockingQueue er det kun den
        // initielle array-størrelsen, ikke en øvre grense, så MAX_VALUE ville prøvd å allokere et enormt array.
        // Køen vokser uansett dynamisk – samme effektive oppførsel som dagens ubegrensede LinkedBlockingQueue.
        return new PriorityBlockingQueue<>(INITIELL_KØKAPASITET, PrioritertProsessinstansOppgave.KOMPARATOR);
    }
}
