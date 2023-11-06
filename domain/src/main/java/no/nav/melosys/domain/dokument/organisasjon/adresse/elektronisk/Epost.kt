package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Epost")
class Epost : ElektroniskAdresse() {
    private var identifikator: String? = null
    fun getIdentifikator(): String? {
        return identifikator
    }

    fun setIdentifikator(identifikator: String?) {
        this.identifikator = identifikator
    }
}
