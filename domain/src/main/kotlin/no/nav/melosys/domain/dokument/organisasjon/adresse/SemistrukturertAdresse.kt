package no.nav.melosys.domain.dokument.organisasjon.adresse

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.dokument.DokumentView
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SemistrukturertAdresse")
class SemistrukturertAdresse : GeografiskAdresse() {
    var adresselinje1: String? = null
    var adresselinje2: String? = null
    var adresselinje3: String? = null
    var postnr: String? = null
    var poststed: String? = null
    var kommunenr: String? = null

    @JsonView(DokumentView.Database::class)
    @XmlElement(name = "poststed_utenlandsk")
    var poststedUtland: String? = null
}
