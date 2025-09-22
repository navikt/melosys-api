package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.MaybePeriode
import no.nav.melosys.domain.SimpleErPeriodeAdapter
import no.nav.melosys.domain.toErPeriode
import java.time.LocalDate

data class Periode(
    override var fom: LocalDate? = null,
    override var tom: LocalDate? = null
) : MaybePeriode {
    // Deprecated: Use toErPeriode() extension function instead
    @Deprecated("Use toErPeriode() extension function from MaybePeriode interface", ReplaceWith("this.toErPeriode()"))
    fun tilErPeriode(): ErPeriode? = toErPeriode()

    override fun toString() = "${fom ?: "null"} → ${tom ?: "null"}"
}
