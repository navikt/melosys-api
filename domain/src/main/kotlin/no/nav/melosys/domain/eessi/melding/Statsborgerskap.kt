package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Statsborgerskap @JsonCreator constructor(
    @JsonProperty("landkode") val landkode: String
)
