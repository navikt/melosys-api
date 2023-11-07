package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Telefonnummer")
class Telefonnummer : ElektroniskAdresse() {
    @JvmField
    var identifikator: String? = null
    var type: String? = null // http://nav.no/kodeverk/Kodeverk/Telefontyper
    var retningsnummer: String? = null // http://nav.no/kodeverk/Kodeverk/Retningsnumre
}
