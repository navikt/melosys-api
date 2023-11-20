package no.nav.melosys.domain.dokument.organisasjon

import no.nav.melosys.domain.dokument.felles.Periode

class Organisasjonsnavn {
    var bruksperiode: Periode? = null
    var gyldighetsperiode: Periode? = null

    var navn: List<String?>? = emptyList()
    var redigertNavn: String? = null
}
