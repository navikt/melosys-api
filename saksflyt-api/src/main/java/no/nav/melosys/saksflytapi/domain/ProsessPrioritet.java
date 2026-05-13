package no.nav.melosys.saksflytapi.domain;

/**
 * ProsessPrioritet for behandling av en {@link Prosessinstans} i saksflyt-køen
 * ({@code saksflytThreadPoolTaskExecutor}).
 *
 * <p><b>Rekkefølgen er signifikant:</b> prioritetskøen sorterer på {@link #ordinal()}, slik at
 * {@link #HØY} plukkes før {@link #NORMAL} før {@link #LAV}. Innen samme prioritet brukes FIFO
 * (eldste {@code registrertDato} først).
 */
public enum ProsessPrioritet {

    /** Saksbehandler-trigget arbeid som en bruker venter på her og nå (iverksett vedtak, journalføring). */
    HØY,

    /** Standard. */
    NORMAL,

    /** Batch / masseopprettelse (årsavregning auto-opprettelse, satsendring) — skal aldri sulte HØY/NORMAL. */
    LAV
}
