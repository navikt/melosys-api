package no.nav.melosys.domain

import java.time.LocalDate

/**
 * Simple, reusable adapter that converts nullable periode data to ErPeriode
 * Eliminates the need for anonymous object creation every time
 */
data class SimpleErPeriodeAdapter(
    override var fom: LocalDate,
    override var tom: LocalDate?
) : ErPeriode
