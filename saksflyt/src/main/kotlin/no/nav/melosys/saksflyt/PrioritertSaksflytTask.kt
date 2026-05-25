package no.nav.melosys.saksflyt

import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import java.time.LocalDateTime
import java.util.Comparator
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue

/**
 * Wrapper rundt behandling av én prosessinstans, lagt på `saksflytThreadPoolTaskExecutor`.
 *
 * Implementerer [Comparable] slik at den underliggende [PriorityBlockingQueue] sorterer på
 * `(prioritet, registrertDato)`: HØY/NORMAL kjøres foran LAV, og FIFO innen samme prioritet.
 * Bruk [PrioritertSaksflytTask.KOMPARATOR] for køen — den er defensiv mot [Runnable]-er som ikke er av denne typen
 * (behandles som [ProsessPrioritet.NORMAL]).
 */
private val log = KotlinLogging.logger { }

class PrioritertSaksflytTask(
    val prosessinstansId: UUID,
    prioritet: ProsessPrioritet?,
    registrertDato: LocalDateTime?,
    private val oppgave: Runnable,
) : Runnable, Comparable<PrioritertSaksflytTask> {

    val prioritet: ProsessPrioritet = prioritet ?: ProsessPrioritet.NORMAL
    val registrertDato: LocalDateTime = registrertDato ?: LocalDateTime.now()

    override fun run() {
        try {
            oppgave.run()
        } catch (e: RuntimeException) {
            // Siste skanse mot at en uventet feil dreper en pooltråd i saksflytThreadPoolTaskExecutor.
            log.error("Uventet feil ved behandling av prosessinstans {}", prosessinstansId, e)
        }
    }

    override fun compareTo(other: PrioritertSaksflytTask): Int = KOMPARATOR.compare(this, other)

    override fun toString(): String =
        "PrioritertSaksflytTask{prosessinstansId=$prosessinstansId, prioritet=$prioritet, registrertDato=$registrertDato}"

    companion object {
        /** Komparator for prioritetskøen. Defensiv: ukjente [Runnable]-er behandles som [ProsessPrioritet.NORMAL]. */
        @JvmField
        val KOMPARATOR: Comparator<Runnable> = Comparator
            .comparingInt<Runnable> { prioritetAv(it).ordinal }
            // Ukjente Runnable-er sorteres bakerst i NORMAL-båndet, så de ikke kan snike seg foran legitimt arbeid.
            .thenComparing { runnable -> (runnable as? PrioritertSaksflytTask)?.registrertDato ?: LocalDateTime.MAX }

        @JvmStatic
        fun prioritetAv(runnable: Runnable): ProsessPrioritet =
            (runnable as? PrioritertSaksflytTask)?.prioritet ?: ProsessPrioritet.NORMAL
    }
}
