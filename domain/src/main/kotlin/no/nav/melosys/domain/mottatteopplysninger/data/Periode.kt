package no.nav.melosys.domain.mottatteopplysninger.data

import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate


class Periode : ErPeriode {
    private var fom: LocalDate? = null
    private var tom: LocalDate? = null

    constructor()
    constructor(fom: LocalDate?, tom: LocalDate?) {
        this.fom = fom
        this.tom = tom
    }

    override fun getFom(): LocalDate? {
        return fom
    }

    override fun getTom(): LocalDate? {
        return tom
    }

    override fun toString(): String {
        return StringBuilder().append(fom).append(" → ").append(tom).toString()
    }
}
