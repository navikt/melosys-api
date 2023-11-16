package no.nav.melosys.domain.dokument.organisasjon.adresse

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StrukturertAdresse")
abstract class StrukturertAdresse : GeografiskAdresse() {
    var tilleggsadresse: String? = null
    var tilleggsadresseType: String? = null
}
