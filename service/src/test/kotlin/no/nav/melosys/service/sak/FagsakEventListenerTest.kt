package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FagsakEventListenerTest {
    @MockK
    lateinit var fagsakService: FagsakService

    @MockK
    lateinit var oppgaveService: OppgaveService

    private lateinit var fagsakEventListener: FagsakEventListener

    @BeforeEach
    fun setUp() {
        fagsakEventListener = FagsakEventListener(fagsakService, oppgaveService)
    }

    @Test
    fun `fagsakEndret - oppgave finnes, oppgave oppdateres`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = lagBehandling(fagsak)
        fagsak.behandlinger.add(behandling)
        val fagsakEndretAvSaksbehandler = FagsakEndretAvSaksbehandler(fagsak.saksnummer)
        val oppgaveID = "oppgaveID"
        val oppgave = Oppgave.Builder().setOppgaveId(oppgaveID).build()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER) } returns Optional.of(oppgave)
        justRun { oppgaveService.oppdaterOppgave(oppgaveID, behandling) }

        fagsakEventListener.fagsakEndret(fagsakEndretAvSaksbehandler)

        verify { oppgaveService.oppdaterOppgave(oppgaveID, behandling) }
    }

    @Test
    fun `fagsakEndret - oppgave finnes ikke, oppgave opprettes`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val fagsakEndretAvSaksbehandler = FagsakEndretAvSaksbehandler(fagsak.saksnummer)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER) } returns Optional.empty()
        justRun { oppgaveService.opprettOppgaveForSak(FagsakTestFactory.SAKSNUMMER) }

        fagsakEventListener.fagsakEndret(fagsakEndretAvSaksbehandler)

        verify { oppgaveService.opprettOppgaveForSak(FagsakTestFactory.SAKSNUMMER) }
    }
}
