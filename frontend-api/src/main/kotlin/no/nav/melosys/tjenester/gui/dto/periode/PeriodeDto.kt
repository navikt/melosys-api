package no.nav.melosys.tjenester.gui.dto.periode

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDate

@JsonPropertyOrder("fom", "tom")
data class PeriodeDto(
    @JsonProperty("fom")
    val fom: LocalDate? = null,

    @JsonProperty("tom")
    val tom: LocalDate? = null
)