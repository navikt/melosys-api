package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.SkjemaSaksstatusSyncService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Synkroniserer saksstatus til melosys-skjema-api. Bestilles fra SkjemaSaksstatusEventListener
 * ved statusendring på fagsaker med skjema-kobling. Kjøres som egen prosessinstans etter at
 * statusendringen er committet, med rekjøringsstøtte fra prosessrammeverket ved feil.
 */
@Component
class SynkSkjemaSaksstatus(
    private val fagsakService: FagsakService,
    private val skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.SYNK_SKJEMA_SAKSSTATUS

    override fun utfør(prosessinstans: Prosessinstans) {
        val saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER)
        val fagsak = fagsakService.hentFagsak(saksnummer)

        log.info { "Synkroniserer saksstatus til melosys-skjema-api for sak $saksnummer" }
        skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak)
    }
}
