package no.nav.melosys.saksflytapi.domain

/**
 * Prioritet for behandling av en [Prosessinstans] i saksflyt-køen (`saksflytThreadPoolTaskExecutor`).
 *
 * **Rekkefølgen er signifikant:** prioritetskøen sorterer på [ordinal], slik at [HØY] plukkes før [NORMAL]
 * før [LAV]. Innen samme prioritet brukes FIFO (eldste `registrertDato` først).
 */
enum class ProsessPrioritet {

    /** Saksbehandler-trigget arbeid som en bruker venter på her og nå (iverksett vedtak, journalføring). */
    HØY,

    /** Standard. */
    NORMAL,

    /** Batch / masseopprettelse (årsavregning auto-opprettelse, satsendring) — skal aldri sulte HØY/NORMAL. */
    LAV;

    /**
     * Returnerer den høyeste av to prioriteter. [HØY] har lavest [ordinal], så naturlig
     * enum-ordning gjør at [minOf] velger den høyeste prioriteten.
     */
    infix fun høyesteAv(annen: ProsessPrioritet): ProsessPrioritet = minOf(this, annen)
}
