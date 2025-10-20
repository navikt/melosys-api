package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

interface Fastsettingsperiode : ErPeriode {
    val periodeFra: LocalDate
    val periodeTil: LocalDate
    val dekning: Trygdedekninger?
    var bestemmelse: Bestemmelse?
    var medlemskapstype: Medlemskapstyper?

    fun erInnvilget(): Boolean
}
