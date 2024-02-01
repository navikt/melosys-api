package no.nav.melosys.tjenester.gui.dto.periode

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDate

@JsonPropertyOrder(
    "fom", "tom"
)
class PeriodeDto {
    @JvmField
    @JsonProperty("fom")
    var fom: LocalDate? = null

    @JvmField
    @JsonProperty("tom")
    var tom: LocalDate? = null

    constructor()

    constructor(fom: LocalDate?, tom: LocalDate?) {
        this.fom = fom
        this.tom = tom
    }
}
