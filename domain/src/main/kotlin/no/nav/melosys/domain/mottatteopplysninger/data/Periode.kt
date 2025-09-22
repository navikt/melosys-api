package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.melosys.domain.MaybePeriode
import java.time.LocalDate

class Periode(
    override var fom: LocalDate? = null,
    override var tom: LocalDate? = null
) : MaybePeriode {
    override fun toString(): String = "$fom → ${tom ?: "∞"}"
}
