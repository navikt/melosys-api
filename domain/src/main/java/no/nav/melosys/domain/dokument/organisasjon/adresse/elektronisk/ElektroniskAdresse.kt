package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk

import no.nav.melosys.domain.dokument.felles.Periode

abstract class ElektroniskAdresse {
    var bruksperiode: Periode? = null
    var gyldighetsperiode: Periode? = null
}
