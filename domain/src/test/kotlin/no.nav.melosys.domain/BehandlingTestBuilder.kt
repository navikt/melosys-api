package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant
import java.time.LocalDate

/**
 * Test-verktøy for å opprette Behandling-instanser med standardverdier.
 */
object BehandlingTestBuilder {

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
        registrertAv = "registrertAv"
        endretDato = Instant.now()
        endretAv = "endretAv"
    }
}

// Utvidelsefunksjon for enda renere syntaks i tester
fun Behandling.Companion.buildForTest(init: Behandling.Builder.() -> Unit = {}): Behandling =
    BehandlingTestBuilder.builderWithDefaults().apply(init).build()
