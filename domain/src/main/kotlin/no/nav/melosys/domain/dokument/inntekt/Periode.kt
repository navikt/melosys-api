package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate

class Periode(
    private var fom: LocalDate? = null,
    private var tom: LocalDate? = null
) : ErPeriode {
    override fun getFom(): LocalDate? = fom
    override fun getTom(): LocalDate? = tom

    override fun toString() = "$fom → $tom"
}
