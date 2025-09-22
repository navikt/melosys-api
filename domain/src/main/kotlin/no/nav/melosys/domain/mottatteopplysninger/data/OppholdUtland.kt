package no.nav.melosys.domain.mottatteopplysninger.data

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.HarPeriode


/**
 * Opplysninger om opphold i utland
 */
class OppholdUtland : HarPeriode {
    var oppholdslandkoder: List<String> = ArrayList()
    var oppholdsPeriode: Periode? = null
    var studentFinansieringKode: String? = null
    var studentSemester: String? = null
    var ektefelleEllerBarnINorge: Boolean? = null

    @JsonIgnore
    override fun getPeriode(): ErPeriode? = oppholdsPeriode?.tilErPeriode()
}
