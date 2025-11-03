package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Trygdedekninger

interface AvgiftspliktigPeriode : ErPeriode {
    fun hentTrygdedekning(): Trygdedekninger
    fun erInnvilget(): Boolean
}
