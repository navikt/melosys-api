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
    BehandlingTestFactory.builderWithDefaults().apply(init).build().knyttTilFagsakOgSaksopplysninger()

private fun Behandling.knyttTilFagsakOgSaksopplysninger(): Behandling = apply {
    fagsak.leggTilBehandling(this)
    // Knytt alle saksopplysninger til behandlingen
    saksopplysninger.forEach { it.behandling = this }
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
 * DSL extension for å konfigurere mottatte opplysninger innenfor en Behandling builder.
 */
fun BehandlingTestFactory.BehandlingTestBuilder.mottatteOpplysninger(
    init: no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerTestFactory.Builder.() -> Unit
) = apply {
    this.mottatteOpplysninger = no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest(init)
}

/**
 * DSL extension for å legge til en saksopplysning innenfor en Behandling builder.
 * Saksopplysningen vil automatisk bli knyttet til behandlingen.
 */
fun BehandlingTestFactory.BehandlingTestBuilder.saksopplysning(init: SaksopplysningTestFactory.Builder.() -> Unit) {
    val saksopplysning = saksopplysningForTest {
        init()
    }
    this.saksopplysninger.add(saksopplysning)
}

/**
 * Test-verktøy for å opprette Behandling-instanser med standardverdier.
 */
object BehandlingTestFactory {
    const val BEHANDLING_ID = 0L

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
        id = BEHANDLING_ID
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
