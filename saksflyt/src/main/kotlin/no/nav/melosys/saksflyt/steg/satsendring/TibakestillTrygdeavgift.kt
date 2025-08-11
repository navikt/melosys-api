package no.nav.melosys.saksflyt.steg.satsendring

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class TibakestillTrygdeavgift(
    private val behandlingsresultatService: BehandlingsresultatService,
) : StegBehandler {
    override fun inngangsSteg() = ProsessSteg.TILBAKESTILL_TRYGDEAVGIFT

    override fun utfør(prosessinstans: Prosessinstans) {
        val aktivBehandling = prosessinstans.behandlingOrFail().fagsak.finnAktivBehandlingIkkeÅrsavregning()

        if (aktivBehandling != null && aktivBehandling.type in aktiveBehandlingstyperSomKanPåvirkes) {
            tilbakestillTrygdeavgift(aktivBehandling.id)
        } else {
            log.warn { "Ingen aktiv ny vurdering eller manglende innbetaling ifm. satsendring for ${prosessinstans.id}. Trygdeavgift tilbakestilles ikke." }
        }
    }

    private fun tilbakestillTrygdeavgift(behandlingID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        behandlingsresultat.clearTrygdeavgiftsperioder()
        log.info { "Trygdeavgiftsperioder tilbakestilt for behandling $behandlingID." }
    }

    companion object {
        // Behandlingstyper som kan påvirkes av satsendringer når de er aktive i en sak.
        val aktiveBehandlingstyperSomKanPåvirkes = setOf(NY_VURDERING, MANGLENDE_INNBETALING_TRYGDEAVGIFT)
    }
}

