package no.nav.melosys.domain

import org.threeten.extra.LocalDateRange
import java.time.LocalDate

/**
 * ## Konverteringsflyt
 *
 * Felles grensesnitt for gyldige perioder med garantert non-null fom-dato.
 *
 * Dette representerer "domene/persistens-laget" for perioder - brukes for domene-entiteter,
 * forretningslogikk og database-lagring hvor fom-dato alltid må være til stede.
 *
 * JSON/DTO → MuligPeriode → validering → ErPeriode → Database
 *
 * @see MuligPeriode.tilErPeriode returnerer null hvis fom mangler
 * @see MuligPeriode.hentErPeriode kaster exception hvis fom mangler
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
