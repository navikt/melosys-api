package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant
import java.time.LocalDate

/**
 * Test utility for creating Behandling instances with sensible defaults.
 * Only use in test code - not in production!
 */
object BehandlingTestBuilder {

    /**
     * Creates a Behandling with sensible defaults for all required fields.
     * Override only the fields you need for your specific test.
     *
     * Example:
     * ```
     * val behandling = BehandlingTestBuilder.build {
     *     status = Behandlingsstatus.AVSLUTTET
     *     tema = Behandlingstema.YRKESAKTIV
     * }
     * ```
     */
    fun build(init: Behandling.Builder.() -> Unit = {}): Behandling = builderWithDefaults().apply(init).build()

    /**
     * Creates a Behandling with all fields set to test values.
     * Useful when you need a "fully populated" entity for testing.
     */
    fun buildComplete(init: Behandling.Builder.() -> Unit = {}): Behandling = builderWithCompleteDefaults().apply(init).build()

    @JvmStatic
    fun builderWithCompleteDefaults() = Behandling.Builder().apply {
        // Required fields
        fagsak = FagsakTestFactory.lagFagsak()
        status = Behandlingsstatus.UNDER_BEHANDLING
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandlingsfrist = LocalDate.now().plusWeeks(12)

        // Optional fields with test values
        sisteOpplysningerHentetDato = Instant.now()
        dokumentasjonSvarfristDato = Instant.now()
        initierendeJournalpostId = "TEST_JP_123"
        initierendeDokumentId = "TEST_DOK_456"
        oppgaveId = "TEST_OPP_789"

        // Collections are empty by default, but initialized
        saksopplysninger = mutableSetOf()
        behandlingsnotater = mutableSetOf()
    }

    /**
     * Creates a minimal Behandling for tests that only need the bare minimum.
     * All non-required fields are left null/empty.
     */
    fun buildMinimal(init: Behandling.Builder.() -> Unit = {}): Behandling = builderWithDefaults().apply(init).build()

    @JvmStatic
    fun builderWithDefaults() = Behandling.Builder().apply {
        // Set sensible defaults for all required fields
        fagsak = FagsakTestFactory.lagFagsak()
        status = Behandlingsstatus.UNDER_BEHANDLING // TODO: Finn beste default verdi her. Før så var denne null
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING // TODO: Finn beste default verdi her. Før så var denne null
        behandlingsfrist = LocalDate.now().plusWeeks(12)

        registrertDato = Instant.now()
        registrertAv = "bla"
        endretDato = Instant.now()
        endretAv = "bla"
    }
}

// Extension function for even cleaner syntax in tests
fun Behandling.Companion.buildForTest(init: Behandling.Builder.() -> Unit = {}): Behandling =
    BehandlingTestBuilder.build(init)

fun Behandling.Companion.buildCompleteForTest(init: Behandling.Builder.() -> Unit = {}): Behandling =
    BehandlingTestBuilder.buildComplete(init)

fun Behandling.Companion.buildMinimalForTest(init: Behandling.Builder.() -> Unit = {}): Behandling =
    BehandlingTestBuilder.buildMinimal(init)
