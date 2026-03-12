package no.nav.melosys.integrasjon.kodeverk.impl.dto

import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import java.time.LocalDate

data class BetydningDto(
    @JsonDeserialize(using = LocalDateDeserializer::class)
    var gyldigFra: LocalDate,
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val gyldigTil: LocalDate,
    val beskrivelser: Map<String, BeskrivelseDto>
)
