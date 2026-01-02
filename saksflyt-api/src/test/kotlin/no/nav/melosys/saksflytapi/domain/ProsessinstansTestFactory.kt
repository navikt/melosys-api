package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.forTest
import java.time.LocalDateTime
import java.util.*

fun Prosessinstans.Companion.forTest(init: Prosessinstans.Builder.() -> Unit = {}): Prosessinstans =
    ProsessinstansTestFactory.builderWithDefaults().apply(init).build()

fun Prosessinstans.Builder.behandling(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit) = apply {
    this.behandling = Behandling.forTest(init)
}

/**
 * Configure the behandling with an opprinneligBehandling.
 * Useful for NY_VURDERING and MANGLENDE_INNBETALING_TRYGDEAVGIFT behandlingstyper.
 *
 * Example:
 * ```
 * Prosessinstans.forTest {
 *     behandling {
 *         type = Behandlingstyper.NY_VURDERING
 *         opprinneligBehandling {
 *             id = 1L
 *         }
 *     }
 * }
 * ```
 */
fun BehandlingTestFactory.BehandlingTestBuilder.medOpprinneligBehandlingId(id: Long) = apply {
    this.opprinneligBehandling = Behandling.forTest { this.id = id }
}

object ProsessinstansTestFactory {
    fun builderWithDefaults() = Prosessinstans.builder().apply {
        id = UUID.randomUUID()
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
        registrertDato = LocalDateTime.now()
        endretDato = LocalDateTime.now()
    }
}
