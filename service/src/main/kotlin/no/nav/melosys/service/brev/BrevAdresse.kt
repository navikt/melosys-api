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

    val isAdresselinjerEmpty: Boolean
        get() = Land_iso2.NO.name != land && (adresselinjer == null || adresselinjer.stream().allMatch { obj: String -> obj.isBlank() })
    val isPostnrEmpty: Boolean
        get() = Land_iso2.NO.name == land && postnr.isNullOrBlank()

}

