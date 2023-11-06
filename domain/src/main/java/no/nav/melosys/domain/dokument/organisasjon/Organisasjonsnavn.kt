package no.nav.melosys.domain.dokument.organisasjon

import no.nav.melosys.domain.dokument.felles.Periode
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper

@XmlAccessorType(XmlAccessType.FIELD)
class Organisasjonsnavn {
    var bruksperiode: Periode? = null
    var gyldighetsperiode: Periode? = null

    @XmlElementWrapper(name = "navn")
    @XmlElement(name = "navnelinje")
    var navn: MutableList<String?>? = ArrayList()
    var redigertNavn: String? = null
}
