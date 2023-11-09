package no.nav.melosys.service.brev

import no.nav.melosys.domain.kodeverk.Land_iso2

data class BrevAdresse(
    val mottakerNavn: String,
    val orgnr: String?,
    val adresselinjer: List<String>?,
    val postnr: String?,
    val poststed: String?,
    val region: String?,
    val land: String?
) {
    fun erUgyldig(): Boolean {
        return isPostnrEmpty() || isAdresselinjerEmpty()
    }

    fun isAdresselinjerEmpty(): Boolean {
        return Land_iso2.NO.name != land && (adresselinjer == null || adresselinjer.all { it.isBlank() })
    }

    fun isPostnrEmpty(): Boolean {
        return Land_iso2.NO.name == land && postnr.isNullOrBlank()
    }
}

