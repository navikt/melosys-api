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

    /**
     * Sjekker om denne perioden er tom (både fom og tom er null).
     * Brukes for å identifisere perioder som ikke er fylt ut ennå.
     *
     * @return true hvis både fom og tom er null
     */
    fun erTom(): Boolean = fom == null && tom == null

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
                dtoPeriode.erTom() -> EmptyErPeriode() // Return empty ErPeriode instead of null
                else -> null // fom null men tom har verdi = ugyldig
            }
        }
    }
}

/**
 * Represents an empty/unspecified period where both fom and tom are conceptually null.
 * Used to preserve backwards compatibility when DTOs have null values but we need ErPeriode.
 */
class EmptyErPeriode : ErPeriode {
    override var fom: LocalDate = LocalDate.MIN  // Sentinel value representing "no date"
    override var tom: LocalDate? = null

    /**
     * An empty period doesn't overlap with any year.
     */
    override fun overlapperMedÅr(år: Int): Boolean = false

    /**
     * An empty period is never valid.
     */
    fun erTom(): Boolean = true

    override fun toString(): String = "EmptyErPeriode(ingen periode spesifisert)"
}
