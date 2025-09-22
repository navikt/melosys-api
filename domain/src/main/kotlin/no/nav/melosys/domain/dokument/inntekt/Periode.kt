package no.nav.melosys.domain.dokument.inntekt

import no.nav.melosys.domain.MuligPeriode
import java.time.LocalDate

data class Periode(
    override var fom: LocalDate? = null,
    override var tom: LocalDate? = null
) : MuligPeriode {

    override fun toString() = "${fom ?: "null"} → ${tom ?: "null"}"
}
