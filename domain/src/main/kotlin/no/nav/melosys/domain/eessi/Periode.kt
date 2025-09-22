package no.nav.melosys.domain.eessi

import java.time.LocalDate

data class Periode(
    var fom: LocalDate? = null,
    var tom: LocalDate? = null
)
