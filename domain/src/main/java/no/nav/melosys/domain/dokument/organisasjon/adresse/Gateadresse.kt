package no.nav.melosys.domain.dokument.organisasjon.adresse

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlType

/**
 * En geografisk adresse som angir geografisk plassering i veiadresse form. Vil brukes om adresser i Norge.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Gateadresse")
class Gateadresse : StrukturertAdresse() {
    @XmlElement(required = true)
    var poststed: String? = null
    var bolignummer: String? = null
    var kommunenummer: String? = null
    var gatenummer: Int? = null

    @JvmField
    @XmlElement(required = true)
    var gatenavn: String? = null
    var husnummer: Int? = null
    var husbokstav: String? = null
}
