package no.nav.melosys.integrasjon.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Periode(
    val fom: String = "",
    val tom: String = ""
)
