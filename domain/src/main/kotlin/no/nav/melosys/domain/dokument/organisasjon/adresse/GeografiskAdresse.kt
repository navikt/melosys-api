package no.nav.melosys.domain.dokument.organisasjon.adresse

import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Landkoder

open class GeografiskAdresse {
    var bruksperiode: Periode? = null
    var gyldighetsperiode: Periode? = null
    var landkode: String? = null

    private fun erNorsk(): Boolean = Landkoder.NO.kode == landkode

    fun erUtenlandsk(): Boolean = !erNorsk()
}
