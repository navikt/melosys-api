package no.nav.melosys.saksflyt.steg.arsavregning

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.stereotype.Component

@Component
class OpprettArsavregning():StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_ARSAVREGNING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans?) {
        println("oppretter årsavregningbehandling")
    }
}
