package no.nav.melosys.saksflyt;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

import no.nav.melosys.saksflytapi.domain.Prioritet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for behandling av én saksflyt-saga som legges på {@code saksflytThreadPoolTaskExecutor}.
 *
 * <p>Implementerer {@link Comparable} slik at den underliggende {@link java.util.concurrent.PriorityBlockingQueue}
 * sorterer på {@code (prioritet, registrertDato)}: HØY/NORMAL kjøres foran LAV, og FIFO innen samme prioritet
 * (eldste {@code registrertDato} først). Bruk {@link #KOMPARATOR} for køen — den er defensiv mot {@link Runnable}-er
 * som ikke er av denne typen (behandles som {@link Prioritet#NORMAL}).
 */
public final class PrioritertProsessinstansOppgave implements Runnable, Comparable<PrioritertProsessinstansOppgave> {

    private static final Logger log = LoggerFactory.getLogger(PrioritertProsessinstansOppgave.class);

    /** Komparator for prioritetskøen. Defensiv: ukjente {@link Runnable}-er behandles som {@link Prioritet#NORMAL}. */
    public static final Comparator<Runnable> KOMPARATOR =
        Comparator.<Runnable>comparingInt(PrioritertProsessinstansOppgave::prioritetOrdinalAv)
            .thenComparing(PrioritertProsessinstansOppgave::registrertDatoAv);

    private final UUID prosessinstansId;
    private final Prioritet prioritet;
    private final LocalDateTime registrertDato;
    private final Runnable oppgave;

    public PrioritertProsessinstansOppgave(UUID prosessinstansId, Prioritet prioritet, LocalDateTime registrertDato, Runnable oppgave) {
        this.prosessinstansId = prosessinstansId;
        this.prioritet = prioritet != null ? prioritet : Prioritet.NORMAL;
        this.registrertDato = registrertDato != null ? registrertDato : LocalDateTime.now();
        this.oppgave = oppgave;
    }

    @Override
    public void run() {
        try {
            oppgave.run();
        } catch (RuntimeException e) {
            // Saksflyt fanger normalt alle feil selv (jf. ProsessinstansBehandler); dette er en siste skanse
            // slik at en uventet feil ikke dreper en pooltråd i saksflytThreadPoolTaskExecutor.
            log.error("Uventet feil ved behandling av prosessinstans {}", prosessinstansId, e);
        }
    }

    @Override
    public int compareTo(PrioritertProsessinstansOppgave annen) {
        return KOMPARATOR.compare(this, annen);
    }

    public Prioritet getPrioritet() {
        return prioritet;
    }

    public UUID getProsessinstansId() {
        return prosessinstansId;
    }

    /** Prioriteten til en {@link Runnable} i køen — {@link Prioritet#NORMAL} for alt som ikke er en {@code PrioritertProsessinstansOppgave}. */
    public static Prioritet prioritetAv(Runnable runnable) {
        return runnable instanceof PrioritertProsessinstansOppgave oppgave ? oppgave.prioritet : Prioritet.NORMAL;
    }

    private static int prioritetOrdinalAv(Runnable runnable) {
        return prioritetAv(runnable).ordinal();
    }

    private static LocalDateTime registrertDatoAv(Runnable runnable) {
        return runnable instanceof PrioritertProsessinstansOppgave oppgave ? oppgave.registrertDato : LocalDateTime.MIN;
    }

    @Override
    public String toString() {
        return "PrioritertProsessinstansOppgave{prosessinstansId=%s, prioritet=%s, registrertDato=%s}".formatted(
            prosessinstansId, prioritet, registrertDato);
    }
}
