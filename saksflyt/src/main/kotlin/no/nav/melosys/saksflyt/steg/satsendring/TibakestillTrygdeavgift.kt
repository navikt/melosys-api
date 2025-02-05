package no.nav.melosys.saksflyt.steg.satsendring

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

@Component
class TibakestillTrygdeavgift(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val behandlingsresultatService: BehandlingsresultatService,
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.TILBAKESTILL_TRYGDEAVGIFT
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        TODO()
    }
}

