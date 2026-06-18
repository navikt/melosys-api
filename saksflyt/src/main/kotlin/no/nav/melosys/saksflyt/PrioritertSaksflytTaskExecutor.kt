package no.nav.melosys.saksflyt

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.BlockingQueue
import java.util.concurrent.PriorityBlockingQueue

/**
 * [ThreadPoolTaskExecutor] som bruker en [PriorityBlockingQueue] i stedet for default FIFO-køen, slik at
 * oppgaver kan plukkes i prioritert rekkefølge (se [PrioritertSaksflytTask]).
 *
 * Bønne-typen forblir [ThreadPoolTaskExecutor] (ikke en bar [java.util.concurrent.ThreadPoolExecutor]), fordi
 * `ProsessinstansService`, `SaksflytHealthIndicator`, `E2ESupportController` m.fl. injiserer den typen og kaller
 * `threadPoolExecutor.queue`.
 */
class PrioritertSaksflytTaskExecutor : ThreadPoolTaskExecutor() {

    // queueCapacity (default Integer.MAX_VALUE) ignoreres bevisst: for en PriorityBlockingQueue er det kun den
    // initielle array-størrelsen, ikke en øvre grense, så MAX_VALUE ville prøvd å allokere et enormt array.
    // Køen vokser uansett dynamisk – samme effektive oppførsel som dagens ubegrensede LinkedBlockingQueue.
    override fun createQueue(queueCapacity: Int): BlockingQueue<Runnable> =
        PriorityBlockingQueue(INITIELL_KØKAPASITET, PrioritertSaksflytTask.KOMPARATOR)

    private companion object {
        const val INITIELL_KØKAPASITET = 16
    }
}
