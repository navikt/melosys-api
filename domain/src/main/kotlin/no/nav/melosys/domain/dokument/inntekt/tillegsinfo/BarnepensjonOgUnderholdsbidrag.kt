package no.nav.melosys.domain.dokument.inntekt.tillegsinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.inntekt.Periode
import no.nav.melosys.domain.toErPeriode

class BarnepensjonOgUnderholdsbidrag : TilleggsinformasjonDetaljer, HarPeriode {
    var forsørgersFødselnummer: String? = null
    var tidsrom: Periode? = null
    @JsonIgnore
    override fun getPeriode(): ErPeriode? {
        return tidsrom?.toErPeriode()
    }
}
