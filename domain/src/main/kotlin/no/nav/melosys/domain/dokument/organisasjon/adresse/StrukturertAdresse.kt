package no.nav.melosys.domain.dokument.organisasjon.adresse

abstract class StrukturertAdresse : GeografiskAdresse() {
    var tilleggsadresse: String? = null
    var tilleggsadresseType: String? = null
}
