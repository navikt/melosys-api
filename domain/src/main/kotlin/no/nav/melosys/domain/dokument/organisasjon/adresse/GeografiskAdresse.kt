package no.nav.melosys.domain.dokument.organisasjon.adresse

import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Landkoder
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlSeeAlso

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(
    SemistrukturertAdresse::class, Gateadresse::class
)
open class GeografiskAdresse {
    var bruksperiode: Periode? = null
    var gyldighetsperiode: Periode? = null
    var landkode: String? = null

    private fun erNorsk(): Boolean = Landkoder.NO.kode == landkode

    fun erUtenlandsk(): Boolean = !erNorsk()
}
