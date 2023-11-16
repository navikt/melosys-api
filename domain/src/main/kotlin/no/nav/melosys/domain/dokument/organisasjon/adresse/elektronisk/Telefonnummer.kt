package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

class Telefonnummer : ElektroniskAdresse() {
    var identifikator: String? = null
    var type: String? = null // http://nav.no/kodeverk/Kodeverk/Telefontyper
    var retningsnummer: String? = null // http://nav.no/kodeverk/Kodeverk/Retningsnumre
}
