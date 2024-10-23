package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Arbeidsland @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("land") val land: String,
    @JsonProperty("arbeidssted") val arbeidssted: List<Arbeidssted> = emptyList(),
) {
}
