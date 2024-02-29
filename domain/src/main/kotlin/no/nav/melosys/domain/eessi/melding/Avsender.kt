package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Avsender @JsonCreator constructor(
    @JsonProperty("avsenderID") val avsenderID: String,
    @JsonProperty("landkode") val landkode: String
)
