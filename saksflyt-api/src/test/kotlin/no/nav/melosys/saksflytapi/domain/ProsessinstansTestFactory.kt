package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.forTest
import java.time.LocalDateTime
import java.util.*

fun Prosessinstans.Companion.forTest(init: ProsessinstansTestFactory.ProsessinstansTestBuilder.() -> Unit = {}): Prosessinstans =
    ProsessinstansTestFactory.ProsessinstansTestBuilder().apply(init).build()

/**
 * Top-level extension for use as `import no.nav.melosys.saksflytapi.domain.behandling`.
 * This allows tests to use `Prosessinstans.forTest { behandling { ... } }` pattern.
 */
fun ProsessinstansTestFactory.ProsessinstansTestBuilder.behandling(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit): ProsessinstansTestFactory.ProsessinstansTestBuilder {
    this.behandling = Behandling.forTest(init)
    return this
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
    val DEFAULT_ID: UUID = UUID.randomUUID()
    val DEFAULT_TYPE = ProsessType.OPPRETT_SAK
    val DEFAULT_STATUS = ProsessStatus.KLAR

    /**
     * Test builder for Prosessinstans with @MelosysTestDsl annotation
     * to prevent scope pollution in nested DSL blocks.
     */
    @MelosysTestDsl
    class ProsessinstansTestBuilder {
        var id: UUID? = UUID.randomUUID()
        var type: ProsessType = DEFAULT_TYPE
        var status: ProsessStatus = DEFAULT_STATUS
        var behandling: Behandling? = null
        var sistFullførtSteg: ProsessSteg? = null
        var registrertDato: LocalDateTime = LocalDateTime.now()
        var endretDato: LocalDateTime = LocalDateTime.now()
        var låsReferanse: String? = null
        private val dataMap = mutableMapOf<ProsessDataKey, Any?>()

        /**
         * Set an existing Behandling instance directly.
         * Use this when you need to reference a pre-configured behandling.
         */
        fun medBehandling(behandling: Behandling) = apply {
            this.behandling = behandling
        }

        fun medData(key: ProsessDataKey, value: Any?) = apply {
            dataMap[key] = value
        }

        fun build(): Prosessinstans {
            val builder = Prosessinstans.builder()
                .medId(id)
                .medType(type)
                .medStatus(status)
                .medBehandling(behandling)
                .medSistFullførtSteg(sistFullførtSteg)
                .medRegistrertDato(registrertDato)
                .medEndretDato(endretDato)
                .medLåsReferanse(låsReferanse)

            dataMap.forEach { (key, value) ->
                builder.medData(key, value)
            }

            return builder.build()
        }
    }
}
