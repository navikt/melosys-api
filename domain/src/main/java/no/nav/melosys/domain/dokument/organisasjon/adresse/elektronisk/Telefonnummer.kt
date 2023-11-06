package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Telefonnummer")
class Telefonnummer : ElektroniskAdresse() {
    private var identifikator: String? = null
    private var type: String? = null // http://nav.no/kodeverk/Kodeverk/Telefontyper
    private var retningsnummer: String? = null // http://nav.no/kodeverk/Kodeverk/Retningsnumre
    fun getIdentifikator(): String? {
        return identifikator
    }

    fun setIdentifikator(identifikator: String?) {
        this.identifikator = identifikator
    }

    fun getType(): String? {
        return type
    }

    fun setType(type: String?) {
        this.type = type
    }

    fun getRetningsnummer(): String? {
        return retningsnummer
    }

    fun setRetningsnummer(retningsnummer: String?) {
        this.retningsnummer = retningsnummer
    }
}
