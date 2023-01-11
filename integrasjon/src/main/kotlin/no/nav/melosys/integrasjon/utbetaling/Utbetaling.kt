package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    var ytelseListe: MutableList<Ytelse>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ytelse(
    var ytelsesperiode: Periode,
    var ytelsestype: String
)
