package no.nav.melosys.service.journalforing.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import no.nav.melosys.saksflytapi.journalfoering.Periode
import java.time.LocalDate

@JsonPropertyOrder("fom", "tom")
data class PeriodeDto(
    @JsonProperty("fom")
    var fom: LocalDate? = null,

    @JsonProperty("tom")
    var tom: LocalDate? = null
) {

    fun tilPeriode(): Periode = Periode(this.fom, this.tom)
}
