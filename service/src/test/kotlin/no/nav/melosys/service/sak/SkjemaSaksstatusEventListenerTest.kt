package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SkjemaSaksstatusEventListenerTest {

    @MockK
    lateinit var skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService

    private lateinit var listener: SkjemaSaksstatusEventListener

    @BeforeEach
    fun setup() {
        listener = SkjemaSaksstatusEventListener(skjemaSaksstatusSyncService)
    }

    private fun lagEvent(fagsak: Fagsak): BehandlingEndretStatusEvent {
        val behandling = Behandling.forTest {
            id = 1
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        return BehandlingEndretStatusEvent(Behandlingsstatus.AVSLUTTET, behandling)
    }

    @Test
    fun `delegerer til sync-servicen med behandlingens fagsak`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) } just runs

        listener.behandlingEndretStatus(lagEvent(fagsak))

        verify(exactly = 1) { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) }
    }

    @Test
    fun `feil fra synkronisering kastes ikke videre og velter ikke saksbehandlingsflyten`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) } throws
            RuntimeException("skjema-api er nede")

        assertDoesNotThrow {
            listener.behandlingEndretStatus(lagEvent(fagsak))
        }
    }
}
