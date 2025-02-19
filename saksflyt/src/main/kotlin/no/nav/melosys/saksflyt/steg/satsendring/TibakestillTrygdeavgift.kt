package no.nav.melosys.saksflyt.steg.satsendring

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

@Component
class TibakestillTrygdeavgift(
    private val behandlingsresultatService: BehandlingsresultatService,
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.TILBAKESTILL_TRYGDEAVGIFT

    override fun utfør(prosessinstans: Prosessinstans) {
        val aktivBehandlingIkkeÅrsavregning = prosessinstans.behandling.fagsak.finnAktivBehandlingIkkeÅrsavregning()

        if (aktivBehandlingIkkeÅrsavregning != null && aktivBehandlingIkkeÅrsavregning.type == NY_VURDERING) {
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(aktivBehandlingIkkeÅrsavregning.id)
            behandlingsresultat.clearTrygdeavgiftsperioder()
        } else {
            log.warn { "Fant ingen aktiv ny vurdering ifm. satsendring. Trygdeavgift tilbakestilles ikke." }
        }
    }
}

