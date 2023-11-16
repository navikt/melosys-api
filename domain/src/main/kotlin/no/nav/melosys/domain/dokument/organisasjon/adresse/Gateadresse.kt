package no.nav.melosys.domain.dokument.organisasjon.adresse

/**
 * En geografisk adresse som angir geografisk plassering i veiadresse form. Vil brukes om adresser i Norge.
 */
class Gateadresse : StrukturertAdresse() {
    var poststed: String? = null // hadde xmk required = true
    var bolignummer: String? = null
    var kommunenummer: String? = null
    var gatenummer: Int? = null

    var gatenavn: String? = null // hadde xmk required = true
    var husnummer: Int? = null
    var husbokstav: String? = null

}
