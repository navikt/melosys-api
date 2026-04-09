package no.nav.melosys.domain.dokument.inntekt.tillegsinfo

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Year

class BonusFraForsvaret : TilleggsinformasjonDetaljer {
    @JsonProperty("åretUtbetalingenGjelderFor")
    var åretUtbetalingenGjelderFor: Year? = null
}
