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
         * Konverterer en DTO periode til ErPeriode, eller returnerer en tom ErPeriode hvis begge datoer er null.
         *
         * Brukes når man må returnere ErPeriode selv om DTO-en har null verdier.
         *
         * @param dtoPeriode DTO periode som kan ha null verdier
         * @return ErPeriode implementation eller null hvis ikke gyldig
         */
        fun tilErPeriodeEllerTom(dtoPeriode: MuligPeriode?): ErPeriode? {
            return when {
                dtoPeriode == null -> null
                dtoPeriode.fom != null -> dtoPeriode.tilErPeriode()
                dtoPeriode.erTom() -> null // Begge null = ingen periode
                else -> null // fom null men tom har verdi = ugyldig
            }
        }
    }
}
