package no.nav.melosys.domain.avgift

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

@JsonIgnoreProperties("fom", "tom")
interface Fastsettingsperiode : ErPeriode {
    val periodeFra: LocalDate
    val periodeTil: LocalDate
    val dekning: Trygdedekninger

    fun erInnvilget(): Boolean
}
