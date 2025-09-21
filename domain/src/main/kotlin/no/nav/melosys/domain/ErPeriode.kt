package no.nav.melosys.domain

import org.threeten.extra.LocalDateRange
import java.time.LocalDate

/**
 * Felles grensesnitt for perioder
 */
interface ErPeriode {

    var fom: LocalDate

    var tom: LocalDate?

    fun erGyldig(): Boolean = inkluderer(LocalDate.now())

    fun inkluderer(kandidat: LocalDate): Boolean {
        if (kandidat.isBefore(fom)) {
            return false
        }
        return tom?.let { !kandidat.isAfter(it) } ?: true
    }

    fun hentTom(): LocalDate = tom ?: error("tom er påkrevd for ${this::class.simpleName}")


    fun overlapperMedÅr(år: Int): Boolean {
        val periodeTom = tom ?: LocalDate.of(år, 12, 31).plusYears(100) // Use far future for open periods
        val localDateRangeForPeriode = LocalDateRange.ofClosed(fom, periodeTom)
        val localDateRangeForÅr = LocalDateRange.ofClosed(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31))
        return localDateRangeForPeriode.overlaps(localDateRangeForÅr)
    }
}
