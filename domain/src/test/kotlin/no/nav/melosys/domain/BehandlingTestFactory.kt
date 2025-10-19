package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant
import java.time.LocalDate

/**
 * Oppretter en Behandling med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val behandling = Behandling.forTest {
 *     status = Behandlingsstatus.AVSLUTTET
 *     tema = Behandlingstema.YRKESAKTIV
 * }
 * ```
 */
fun Behandling.Companion.forTest(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}): Behandling =
    BehandlingTestFactory.builderWithDefaults().apply(init).build().knyttTilFagsak()

private fun Behandling.knyttTilFagsak(): Behandling = apply {
    fagsak.leggTilBehandling(this)
}

/**
 * DSL extension for å konfigurere fagsak innenfor en Behandling builder.
 */
fun BehandlingTestFactory.BehandlingTestBuilder.fagsak(init: FagsakTestFactory.Builder.() -> Unit) = apply {
    this.fagsak = Fagsak.forTest(init)
}

/**
 * DSL extension for å sette behandlingsårsak med kun type.
 * Oppretter en Behandlingsaarsak med standardverdier for fritekst og mottaksdato.
 */
fun BehandlingTestFactory.BehandlingTestBuilder.medBehandlingsårsakType(
    type: Behandlingsaarsaktyper,
) = apply {
    medBehandlingsårsak(Behandlingsaarsak(type, "", LocalDate.now()))
}

/**
 * Test-verktøy for å opprette Behandling-instanser med standardverdier.
 */
object BehandlingTestFactory {

    /**
     * @MelosysTestDsl forhindrer at man kan aksessere properties fra ytre scopes,
     * f.eks. at man setter Fagsak-properties inne i en behandling { } block.
     */
    @MelosysTestDsl
    class BehandlingTestBuilder : Behandling.Builder()

    /**
     * Oppretter en Behandling med fornuftige standardverdier for alle påkrevde felt.
     * Overstyr kun de feltene du trenger for din spesifikke test.
     *
     * Eksempel:
     * ```
     * BehandlingTestFactory.builderWithDefaults()
     *     .medId(1L)
     *     .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
     *     .build();
     * ```
     */
    @JvmStatic
    fun builderWithDefaults() = BehandlingTestBuilder().apply {
        // Sett standardverdier for alle påkrevde felt
        fagsak = FagsakTestFactory.lagFagsak()

        // Finn beste standardverdi på status, type og tema - før var disse null
        status = Behandlingsstatus.UNDER_BEHANDLING
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING

        behandlingsfrist = LocalDate.now().plusWeeks(12)

        registrertDato = Instant.now()
        endretDato = Instant.now()
    }
}
