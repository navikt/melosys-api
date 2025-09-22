package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.MaybePeriode
import no.nav.melosys.domain.SimpleErPeriodeAdapter
import no.nav.melosys.domain.toErPeriode
import java.time.LocalDate


class Periode(
    override var fom: LocalDate? = null,
    override var tom: LocalDate? = null
) : MaybePeriode {
    override fun toString(): String = "$fom → ${tom ?: "∞"}"

    // Deprecated: Use toErPeriode() extension function instead
    @Deprecated("Use toErPeriode() extension function from MaybePeriode interface", ReplaceWith("this.toErPeriode()"))
    fun tilErPeriode(): ErPeriode? = toErPeriode()
}
