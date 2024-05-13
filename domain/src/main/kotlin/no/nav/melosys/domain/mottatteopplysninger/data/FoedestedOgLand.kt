package no.nav.melosys.domain.mottatteopplysninger.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class FoedestedOgLand @JsonCreator constructor(
    @JsonProperty("foedested")
    val foedested: String,
    @JsonProperty("foedeland")
    val foedeland: String
)
