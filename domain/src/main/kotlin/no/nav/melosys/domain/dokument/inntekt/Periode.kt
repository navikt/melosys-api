package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate

data class Periode(
    var fom: LocalDate? = null,
    var tom: LocalDate? = null
) {
    fun tilErPeriode(): ErPeriode? = fom?.let { fomDate ->
        object : ErPeriode {
            override var fom: LocalDate = fomDate
            override var tom: LocalDate? = this@Periode.tom
        }
    }

    override fun toString() = "${fom ?: "null"} → ${tom ?: "null"}"
}
