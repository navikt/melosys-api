package no.nav.melosys.domain.dokument.organisasjon.adresse

/**
 * En geografisk adresse som angir geografisk plassering i veiadresse form. Vil brukes om adresser i Norge.
 */
class Gateadresse : StrukturertAdresse() {
    var poststed: String? = null // hadde xml required = true - TODO: se om vi kan bruke non-null
    var bolignummer: String? = null
    var kommunenummer: String? = null
    var gatenummer: Int? = null

    var gatenavn: String? = null // hadde xml required = true - TODO: se om vi kan bruke non-null
    var husnummer: Int? = null
    var husbokstav: String? = null

}
