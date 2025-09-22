package no.nav.melosys.domain.dokument.inntekt.tillegsinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.inntekt.Periode
import no.nav.melosys.domain.toErPeriode

class Etterbetalingsperiode : TilleggsinformasjonDetaljer, HarPeriode {
    var etterbetalingsperiode: Periode? = null
    @JsonIgnore
    override fun getPeriode(): ErPeriode? {
        return etterbetalingsperiode?.toErPeriode()
    }
}
