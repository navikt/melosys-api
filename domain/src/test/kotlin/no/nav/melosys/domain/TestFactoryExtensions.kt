package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.avgift.ÅrsavregningTestFactory
import no.nav.melosys.domain.avgift.forTest

/**
 * Cross-factory extensions that link different test factory builders.
 * These are consolidated here to avoid circular dependencies and make discovery easier.
 *
 * Pattern: Extensions that allow building one domain object within another domain object's builder.
 */

// ====== Årsavregning <-> Behandlingsresultat extensions ======

/**
 * Extension for building behandlingsresultat inside Årsavregning.forTest { }
 *
 * Example:
 * ```
 * Årsavregning.forTest {
 *     behandlingsresultat {
 *         type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
 *     }
 * }
 * ```
 */
fun ÅrsavregningTestFactory.Builder.behandlingsresultat(init: BehandlingsresultatTestFactory.Builder.() -> Unit) = apply {
    this.behandlingsresultat = Behandlingsresultat.forTest(init)
}

/**
 * Extension for building årsavregning inside Behandlingsresultat.forTest { }
 *
 * Example:
 * ```
 * Behandlingsresultat.forTest {
 *     årsavregning {
 *         aar = 2024
 *     }
 * }
 * ```
 */
fun BehandlingsresultatTestFactory.Builder.årsavregning(init: ÅrsavregningTestFactory.Builder.() -> Unit) = apply {
    this.årsavregning = Årsavregning.forTest(init)
}
