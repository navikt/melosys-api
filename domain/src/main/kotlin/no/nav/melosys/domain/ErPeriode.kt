package no.nav.melosys.domain

import org.threeten.extra.LocalDateRange
import java.time.LocalDate

/**
 * Felles grensesnitt for gyldige perioder med garantert non-null fom-dato.
 *
 * Dette representerer "domene/persistens-laget" for perioder - brukes for domene-entiteter,
 * forretningslogikk og database-lagring hvor fom-dato alltid må være til stede.
 *
 * ## Two-Tier Period Architecture
 *
 * Systemet bruker to distinkte periode-konsepter:
 * 1. **MuligPeriode** - Transport/DTO-lag med nullable datoer
 * 2. **ErPeriode** (dette grensesnittet) - Domene/Persistens-lag med garantert non-null fom
 *
 * ### Når bruke ErPeriode:
 * - Domene-entiteter og forretningslogikk
 * - Database-persistering (alle entiteter krever non-null fom)
 * - Etter validering av at fom er til stede
 * - Ved behandling av gyldige perioder i systemet
 *
 * ### Kontrakt:
 * - `fom` er ALLTID non-null (garantert av typesystemet)
 * - `tom` kan være null (åpen periode)
 * - Alle database-entiteter som `Medlemskapsperiode`, `Lovvalgsperiode` osv. har `@Column(nullable = false)` på fom
 *
 * ### Konvertering fra MuligPeriode:
 * ```kotlin
 * val muligPeriode: MuligPeriode = ... // Fra DTO/JSON
 * val erPeriode: ErPeriode? = muligPeriode.tilErPeriode() // null hvis fom er null
 * val erPeriodeMedFeil: ErPeriode = muligPeriode.hentErPeriode() // kaster exception hvis fom er null
 * ```
 *
 * @see MuligPeriode for perioder som kan ha nullable fom/tom (transport-lag)
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
