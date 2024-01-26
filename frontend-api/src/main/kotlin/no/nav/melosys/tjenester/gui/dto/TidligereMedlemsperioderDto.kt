package no.nav.melosys.tjenester.gui.dto

import com.fasterxml.jackson.annotation.JsonProperty

class TidligereMedlemsperioderDto {
    @JvmField
    @JsonProperty("tidligere_medlemsperiode_ids")
    var periodeIder: List<Long>? = null
}
