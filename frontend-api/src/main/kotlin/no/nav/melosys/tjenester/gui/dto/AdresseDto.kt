package no.nav.melosys.tjenester.gui.dto

class AdresseDto {
    @JvmField
    var gateadresse: GateadresseDto? = null

    @JvmField
    var postnr: String? = null

    @JvmField
    var poststed: String? = null

    @JvmField
    var land: String? = null
}
