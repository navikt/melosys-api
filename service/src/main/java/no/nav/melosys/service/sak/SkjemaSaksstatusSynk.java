package no.nav.melosys.service.sak;

/**
 * Styrer om en fagsak-statusendring skal utløse synkronisering av saksstatus til
 * melosys-skjema-api (for saker med skjema-kobling).
 *
 * Ingen defaultverdi med vilje: hvert kallsted må ta eksplisitt stilling, slik at nye kallsteder
 * ikke stille kan bryte SAGA-prinsippet om at prosessinstans-steg ikke bestiller barneprosesser.
 *
 * <p>OBS: også ren BEHANDLINGS-lukking uten fagsak-statusendring krever synk-stillingtagen —
 * utledet skjema-status avhenger av om saken har aktiv behandling, så en sti som kun lukker en
 * behandling passerer aldri FagsakService.oppdaterStatus og trigger dermed ingen event. Slike
 * stier må enten sette SYNK_SAKSSTATUS_SAKSNUMMER-markøren (prosessinstans-steg, jf.
 * Prosessinstans.markerForSkjemaSaksstatusSynk) eller kalle
 * SkjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet (REST-/admin-stier). Glemmes dette i en
 * fremtidig kun-behandling-sti, er den idempotente massesynken
 * (/admin/skjema-saksstatus/synk) sikkerhetsnettet.
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
