package no.nav.melosys.domain.dokument.utbetaling

import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate

data class Periode(
    private val fom: LocalDate? = null,
    private val tom: LocalDate? = null
) : ErPeriode {
    override fun getFom(): LocalDate? = fom
    override fun getTom(): LocalDate? = tom

    override fun toString() = "$fom → $tom"
}
