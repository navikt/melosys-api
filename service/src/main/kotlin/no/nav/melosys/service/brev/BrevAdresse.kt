package no.nav.melosys.service.brev

import no.nav.melosys.domain.kodeverk.Land_iso2

data class BrevAdresse(
    val mottakerNavn: String,
    val orgnr: String?,
    val adresselinjer: List<String>?,
    val postnr: String?,
    val poststed: String?,
    val region: String?,
    val land: String?,
    var ugyldig: Boolean,
) {
    constructor(
        mottakerNavn: String,
        orgnr: String?,
        adresselinjer: List<String>?,
        postnr: String?,
        poststed: String?,
        region: String?,
        land: String?
    ) : this(mottakerNavn, orgnr, adresselinjer, postnr, poststed, region, land, false) {
        ugyldig = harIkkePostnr() || harIngenAdresselinjer()
    }

    fun harIngenAdresselinjer(): Boolean {
        return Land_iso2.NO.name != land && (adresselinjer == null || adresselinjer.all { it.isBlank() })
    }

    fun harIkkePostnr(): Boolean {
        return Land_iso2.NO.name == land && postnr.isNullOrBlank()
    }
}

