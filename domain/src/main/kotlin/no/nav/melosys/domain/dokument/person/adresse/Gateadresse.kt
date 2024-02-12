package no.nav.melosys.domain.dokument.person.adresse

import com.fasterxml.jackson.annotation.JsonIgnore

data class Gateadresse(
    var gatenavn: String? = null,
    var gatenummer: Int? = null,
    var husnummer: Int? = null,
    var husbokstav: String? = null
) {
    @JsonIgnore
    fun erTom(): Boolean = gatenavn.isNullOrEmpty() && husbokstav.isNullOrEmpty() && gatenummer == null && husnummer == null
}

