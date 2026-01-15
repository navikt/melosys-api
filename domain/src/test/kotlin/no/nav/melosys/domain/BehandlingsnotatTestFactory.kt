package no.nav.melosys.domain

import java.time.Instant

/**
 * Oppretter en Behandlingsnotat med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val notat = behandlingsnotatForTest {
 *     tekst = "Min test-notat"
 *     behandling { status = Behandlingsstatus.UNDER_BEHANDLING }
 * }
 * ```
 */
fun behandlingsnotatForTest(init: BehandlingsnotatTestFactory.Builder.() -> Unit = {}): Behandlingsnotat =
    BehandlingsnotatTestFactory.builderWithDefaults().apply(init).build()

/**
 * DSL extension for å konfigurere behandling innenfor en Behandlingsnotat builder.
 */
fun BehandlingsnotatTestFactory.Builder.behandling(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit) = apply {
    this.behandling = Behandling.forTest(init)
}

object BehandlingsnotatTestFactory {
    const val DEFAULT_TEKST = "Test notat"
    const val DEFAULT_SAKSBEHANDLER = "Z999999"

    @MelosysTestDsl
    class Builder {
        var id: Long = 1L
        var tekst: String = DEFAULT_TEKST
        var behandling: Behandling = Behandling.forTest { }
        var registrertAv: String = DEFAULT_SAKSBEHANDLER
        var endretAv: String = DEFAULT_SAKSBEHANDLER
        var registrertDato: Instant = Instant.now()
        var endretDato: Instant = Instant.now()

        fun build(): Behandlingsnotat = Behandlingsnotat().apply {
            this.id = this@Builder.id
            this.tekst = this@Builder.tekst
            this.behandling = this@Builder.behandling
            this.registrertAv = this@Builder.registrertAv
            this.endretAv = this@Builder.endretAv
            this.registrertDato = this@Builder.registrertDato
            this.endretDato = this@Builder.endretDato
        }
    }

    fun builderWithDefaults() = Builder()
}
