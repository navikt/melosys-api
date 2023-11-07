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
    @JvmField
    var adresselinje1: String? = null
    @JvmField
    var adresselinje2: String? = null
    @JvmField
    var adresselinje3: String? = null
    @JvmField
    var postnr: String? = null
    @JvmField
    var poststed: String? = null
    @JvmField
    var kommunenr: String? = null

    @JvmField
    @JsonView(DokumentView.Database::class)
    @XmlElement(name = "poststed_utenlandsk")
    var poststedUtland: String? = null
}
