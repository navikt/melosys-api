package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate


class Periode(
    var fom: LocalDate?,
    var tom: LocalDate?
) {
    override fun toString(): String = "$fom → ${tom ?: "∞"}"

    // Convert to ErPeriode when fom is available
    fun tilErPeriode(): ErPeriode? = if (fom != null) {
        object : ErPeriode {
            override var fom: LocalDate = this@Periode.fom!!
            override var tom: LocalDate? = this@Periode.tom
        }
    } else null
}

