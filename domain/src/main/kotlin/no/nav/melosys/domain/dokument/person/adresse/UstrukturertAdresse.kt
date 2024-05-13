package no.nav.melosys.domain.dokument.person.adresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.dokument.felles.Land

data class UstrukturertAdresse(
    var adresselinje1: String? = null,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adresselinje4: String? = null,
    var postnr: String? = null,
    var poststed: String? = null,
    var land: Land? = null
) {
    @JsonIgnore
    fun erTom(): Boolean = listOf(adresselinje1, adresselinje2, adresselinje3, adresselinje4, postnr, poststed).all { it.isNullOrEmpty() } && land == null

    @JsonIgnore
    fun adresselinjer(): List<String?> = listOf(adresselinje1, adresselinje2, adresselinje3, adresselinje4).filterNotNull()
}

