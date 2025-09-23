package no.nav.melosys.domain

import java.time.LocalDate

/**
 * Interface for period-like objects that may have nullable fom/tom dates.
 *
 * This represents the "transport layer" for periods - used for DTOs, JSON deserialization,
 * and external data where period information might be missing or incomplete.
 *
 * ## Two-Tier Period Architecture
 *
 * The system uses two distinct period concepts:
 * 1. **MuligPeriode** (this interface) - Transport/DTO layer with nullable dates
 * 2. **ErPeriode** - Domain/Persistence layer requiring non-null fom
 *
 * ### When to use MuligPeriode:
 * - Receiving data from external systems or JSON APIs
 * - DTOs where period data might be incomplete
 * - Intermediate processing before validation
 *
 * ### When to use ErPeriode:
 * - Domain entities and business logic
 * - Database persistence (all entities enforce non-null fom)
 * - After validation that fom is present
 *
 * @see ErPeriode for periods with guaranteed non-null fom
 */
interface MuligPeriode {
    val fom: LocalDate?
    val tom: LocalDate?

    /**
     * Konverterer MuligPeriode til ErPeriode hvis fom ikke er null.
     * Brukes når man trenger å validere og konvertere fra transport- til domenelag.
     *
     * @return ErPeriode implementation eller null hvis fom er null
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

    /**
     * Sjekker om denne perioden er gyldig for konvertering til ErPeriode.
     * En periode er gyldig hvis den har minst en fom-dato.
     *
     * @return true hvis perioden kan konverteres til ErPeriode
     */
    fun erGyldig(): Boolean = fom != null

}
