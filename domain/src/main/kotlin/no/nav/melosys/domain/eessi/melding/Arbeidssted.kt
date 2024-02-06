package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Arbeidssted @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("navn") val navn: String,
    @JsonProperty("adresse") val adresse: Adresse,
    @JsonProperty("hjemmebase") val hjemmebase: String? = null,
    @JsonProperty("erIkkeFastAdresse") val erIkkeFastAdresse: Boolean = false
) {
    constructor(navn: String, adresse: Adresse) : this(navn, adresse, null, false)
}
