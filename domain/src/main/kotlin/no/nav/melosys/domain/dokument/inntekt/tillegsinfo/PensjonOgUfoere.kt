package no.nav.melosys.domain.dokument.inntekt.tillegsinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.inntekt.Periode
import java.math.BigDecimal

class PensjonOgUfoere : TilleggsinformasjonDetaljer, HarPeriode {
    @JsonProperty("grunnpensjonbeloep")
    var grunnpensjonbeløp: BigDecimal? = null
    var heravEtterlattepensjon: BigDecimal? = null
    var pensjonsgrad: Int? = null
    var tidsrom: Periode? = null

    @JsonProperty("tillegspensjonbeloep")
    var tillegspensjonbeløp: BigDecimal? = null

    @JsonProperty("ufoeregradpensjonsgrad")
    var uføreEllerPensjonsgrad: Int? = null

    @JsonIgnore
    override fun getPeriode(): ErPeriode? {
        return tidsrom?.toErPeriode()
    }
}
