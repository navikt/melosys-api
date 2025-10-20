package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

interface Fastsettingsperiode : ErPeriode {
    val periodeFra: LocalDate
    val periodeTil: LocalDate
    val dekning: Trygdedekninger

    fun erInnvilget(): Boolean
}
