package no.nav.melosys.service.sak;

/**
 * Styrer om en fagsak-statusendring skal utløse synkronisering av saksstatus til
 * melosys-skjema-api (for saker med skjema-kobling).
 *
 * Ingen defaultverdi med vilje: hvert kallsted må ta eksplisitt stilling, slik at nye kallsteder
 * ikke stille kan bryte SAGA-prinsippet om at prosessinstans-steg ikke bestiller barneprosesser.
 *
 * <p>Utledet skjema-status er en ren funksjon av fagsakstatus (produkteierbeslutning
 * 2026-07-21) — kun fagsak-statusendringer trigger synk. Ren behandlingslukking uten
 * fagsak-statusendring krever ingen synk. Den idempotente massesynken
 * (/admin/skjema-saksstatus/synk) er sikkerhetsnettet ved drift/avvik.
 */
public enum SkjemaSaksstatusSynk {

    /**
     * Endringen skjer utenfor en prosessinstans (REST/admin/scheduler): publiser
     * FagsakStatusEndretEvent slik at en SYNK_SKJEMA_SAKSSTATUS-prosess bestilles for saker
     * med skjema-mapping.
     */
    SYNKRONISER,

    /**
     * Endringen skjer i et prosessinstans-steg: flyten eier selv SYNK_SKJEMA_SAKSSTATUS-steget,
     * så ingen event publiseres (et prosessinstans-steg skal ikke bestille barneprosesser).
     */
    HÅNDTERES_AV_PROSESSFLYT
}
