package no.nav.melosys.service.oppgave.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDate

@JsonPropertyOrder("fom", "tom")
data class PeriodeDto(
    @JsonProperty("fom")
    var fom: LocalDate? = null,

    @JsonProperty("tom")
    var tom: LocalDate? = null
)
