package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.sak.SkjemaSaksstatusSyncService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Synkroniserer saksstatus til melosys-skjema-api. Ligger som SISTE steg i alle flyter som
 * avslutter sak/behandling, i den frittstående SYNK_SKJEMA_SAKSSTATUS-flyten bestilt fra
 * SkjemaSaksstatusEventListener, og sist i MOTTAK_SED-flyten.
 *
 * Steget synker KUN når [ProsessDataKey.SYNK_SAKSSTATUS_SAKSNUMMER] er satt — nøkkelen settes
 * eksplisitt (via Prosessinstans.markerForSkjemaSaksstatusSynk) av stegene som endrer
 * fagsakstatus (AvsluttFagsakOgBehandling i grenene som lukker saken, og SED-rutingens
 * annullering). Ingen fallback til prosessinstansens behandling: SED-ruterne
 * setter behandling på instansen under ruting, og en fallback ville gitt et reelt
 * skjema-api-kall for hver innkommende SED på en skjema-koblet sak (og latt skjema-api-nedetid
 * feile SED-mottak). Uten nøkkelen er steget deterministisk no-op.
 *
 * Steget ligger sist i flytene slik at alle forretningskritiske steg er fullført før synken —
 * en synk-feil (f.eks. skjema-api-nedetid) feiler da instansen isolert og kan rekjøres uten å
 * berøre vedtaks-/avsluttingsstegene. Ulempen er at feil i mellomsteg utsetter synken til
 * rekjøring, og at prosessdata persisteres først etter steget (lite crash-vindu i SED-ruting) —
 * den idempotente massesynken (/admin/skjema-saksstatus/synk) er sikkerhetsnettet for begge.
 */
@Component
class SynkSkjemaSaksstatus(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SYNK_SKJEMA_SAKSSTATUS

    override fun utfør(prosessinstans: Prosessinstans) {
        val saksnummer = prosessinstans.getData(ProsessDataKey.SYNK_SAKSSTATUS_SAKSNUMMER)

        if (saksnummer == null) {
            log.debug { "SYNK_SAKSSTATUS_SAKSNUMMER-prosessdata er ikke satt — hopper over saksstatus-synk" }
            return
        }

        skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer)
    }
}
