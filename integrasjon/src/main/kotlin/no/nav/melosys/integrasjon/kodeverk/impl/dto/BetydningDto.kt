package no.nav.melosys.integrasjon.kodeverk.impl.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate

class BetydningDto(
    @JsonDeserialize(using = LocalDateDeserializer::class)
    var gyldigFra: LocalDate,
    @JsonDeserialize(using = LocalDateDeserializer::class)
    var gyldigTil: LocalDate,
    var beskrivelser: Map<String, BeskrivelseDto>
)
