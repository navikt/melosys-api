package no.nav.melosys.domain

import java.time.LocalDate

/**
 * Interface for period-like objects that may have nullable fom/tom dates.
 *
 * This is primarily used for DTOs and JSON deserialization where period data
 * might be missing or incomplete. When you have guaranteed non-null data,
 * use ErPeriode instead.
 *
 * @see ErPeriode for periods with guaranteed non-null fom
 */
interface MuligPeriode {
    val fom: LocalDate?
    val tom: LocalDate?

    /**
     * Converts a MaybePeriode to ErPeriode if fom is not null.
     *
     * @return ErPeriode implementation or null if fom is null
     */
    fun tilErPeriode(): ErPeriode? = fom?.let { fomDate ->
        SimpleErPeriodeAdapter(fomDate, tom)
    }

    /**
     * Henter ErPeriode fra MaybePeriode. Kaster exception hvis fom er null.
     *
     * @return ErPeriode implementation
     * @throws IllegalStateException hvis fom er null
     */
    fun hentErPeriode(): ErPeriode = tilErPeriode()
        ?: error("Kan ikke opprette ErPeriode: fom-dato er påkrevd men er null")
}
