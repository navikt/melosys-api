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
     * Sjekker om denne perioden er tom (både fom og tom er null).
     * Brukes for å identifisere perioder som ikke er fylt ut ennå.
     *
     * @return true hvis både fom og tom er null
     */
    fun erTom(): Boolean = fom == null && tom == null

    /**
     * Sjekker om denne perioden er gyldig for konvertering til ErPeriode.
     * En periode er gyldig hvis den har minst en fom-dato.
     *
     * @return true hvis perioden kan konverteres til ErPeriode
     */
    fun erGyldig(): Boolean = fom != null

    companion object {
        /**
         * Oppretter en tom periode for JSON lagring hvor både fom og tom er null.
         *
         * Brukes når:
         * - Man ikke har periode-informasjon ennå
         * - JSON trenger en periode-struktur men ingen datoer er satt
         * - Man vil representere "ingen periode spesifisert" tilstand
         *
         * @return Periode med null verdier for både fom og tom
         */
        fun tomPeriode(): no.nav.melosys.domain.dokument.inntekt.Periode =
            no.nav.melosys.domain.dokument.inntekt.Periode(fom = null, tom = null)

        /**
         * Konverterer en DTO periode til ErPeriode, og returnerer en tom ErPeriode hvis begge datoer er null.
         * Dette bevarer original oppførsel hvor man alltid får en periode-objekt tilbake.
         *
         * @param dtoPeriode DTO periode som kan ha null verdier
         * @return ErPeriode implementation, aldri null (EmptyErPeriode for tomme perioder)
         */
        fun tilErPeriodeEllerTom(dtoPeriode: MuligPeriode?): ErPeriode? {
            return when {
                dtoPeriode == null -> null
                dtoPeriode.fom != null -> dtoPeriode.tilErPeriode()
                // When both are null, return the original DTO wrapped to preserve exact behavior
                dtoPeriode.erTom() -> PeriodeAdapter(dtoPeriode as no.nav.melosys.domain.dokument.inntekt.Periode)
                else -> null // fom null men tom har verdi = ugyldig
            }
        }
    }
}

/**
 * Adapter that wraps a DTO Periode and makes it behave like ErPeriode while preserving exact original values.
 * This preserves backwards compatibility - production code gets the exact same null values as before.
 */
class PeriodeAdapter(private val originalPeriode: no.nav.melosys.domain.dokument.inntekt.Periode) : ErPeriode {

    override var fom: LocalDate
        get() = originalPeriode.fom ?: throw IllegalStateException("fom er null i originalPeriode - dette kan skje når DTO har null verdier")
        set(value) { originalPeriode.fom = value }

    override var tom: LocalDate?
        get() = originalPeriode.tom
        set(value) { originalPeriode.tom = value }

    /**
     * When fom is null, no meaningful overlap can be calculated.
     */
    override fun overlapperMedÅr(år: Int): Boolean = false

    override fun toString(): String = "PeriodeAdapter(${originalPeriode.fom} → ${originalPeriode.tom})"

    /**
     * Provides access to the original DTO with null values preserved.
     */
    fun hentOriginalPeriode(): no.nav.melosys.domain.dokument.inntekt.Periode = originalPeriode
}
