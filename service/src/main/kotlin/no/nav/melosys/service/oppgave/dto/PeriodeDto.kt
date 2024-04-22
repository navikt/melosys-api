package no.nav.melosys.service.oppgave.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDate

@JsonPropertyOrder("fom", "tom")
data class PeriodeDto(
    @JsonProperty("fom")
    val fom: LocalDate?,

    @JsonProperty("tom")
    val tom: LocalDate?
)
