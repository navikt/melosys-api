package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class Arbeidssted @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("navn") val navn: String? = null,
    @JsonProperty("adresse") val adresse: Adresse,
    @JsonProperty("hjemmebase") val hjemmebase: String? = null,
    @JsonProperty("erIkkeFastAdresse") @JsonSetter(nulls = Nulls.SKIP) val erIkkeFastAdresse: Boolean = false
)
