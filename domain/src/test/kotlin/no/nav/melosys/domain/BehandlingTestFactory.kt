package no.nav.melosys.domain

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
fun Behandling.Companion.forTest(init: Behandling.Builder.() -> Unit = {}): Behandling =
    BehandlingTestFactory.builderWithDefaults().apply(init).build().knyttTilFagsak()

private fun Behandling.knyttTilFagsak(): Behandling = apply {
    fagsak.leggTilBehandling(this)
}

/**
 * Test-verktøy for å opprette Behandling-instanser med standardverdier.
 */
object BehandlingTestFactory {

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
    fun builderWithDefaults() = Behandling.Builder().apply {
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
