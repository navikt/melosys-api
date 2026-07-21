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
 * Synkroniserer saksstatus til melosys-skjema-api. Ligger som eget steg i alle flyter som
 * avslutter sak/behandling (rett etter AVSLUTT_SAK_OG_BEHANDLING), i den frittstående
 * SYNK_SKJEMA_SAKSSTATUS-flyten bestilt fra SkjemaSaksstatusEventListener, og sist i
 * MOTTAK_SED-flyten (der SED-rutingen setter SAKSNUMMER-prosessdata ved annullering).
 *
 * Saksnummer hentes fra SAKSNUMMER-prosessdata, ellers fra prosessinstansens behandling
 * (samme kilde som AvsluttFagsakOgBehandling-steget bruker for fagsaken). Finnes ingen av
 * delene, eller saken ikke har skjema-mapping, er steget no-op. Feil gir rekjøringsstøtte
 * fra prosessrammeverket.
 */
@Component
class SynkSkjemaSaksstatus(
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SYNK_SKJEMA_SAKSSTATUS

    override fun utfør(prosessinstans: Prosessinstans) {
        val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
            ?: prosessinstans.behandling?.fagsak?.saksnummer

        if (saksnummer == null) {
            log.debug { "Verken SAKSNUMMER-prosessdata eller behandling er satt — hopper over saksstatus-synk" }
            return
        }

        skjemaSaksstatusSyncService.synkroniserSaksstatusForSaksnummer(saksnummer)
    }
}
