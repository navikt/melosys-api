package no.nav.melosys.domain.dokument.organisasjon.adresse

/**
 * En geografisk adresse som angir geografisk plassering i veiadresse form. Vil brukes om adresser i Norge.
 */
class Gateadresse(
    var poststed: String,
    var bolignummer: String? = null,
    var kommunenummer: String? = null,
    var gatenummer: Int? = null,

    var gatenavn: String,
    var husnummer: Int? = null,
    var husbokstav: String? = null
) : StrukturertAdresse()
