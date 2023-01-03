package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Utbetaling(
    var ytelseListe: MutableList<Ytelse> = mutableListOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ytelse(
    var ytelsesperiode: Periode = Periode("",""),
    var ytelsestype: String = ""
)
