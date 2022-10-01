package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.SaksbehandlingDataFactory.lagFagsak
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

private const val SAKSNUMMER = "MEL-0"

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
        val fagsak = lagFagsak(SAKSNUMMER)
        fagsak.behandlinger.add(lagBehandling(fagsak))
        val fagsakEndretAvSaksbehandler = FagsakEndretAvSaksbehandler(fagsak.saksnummer)
        val oppgaveID = "oppgaveID"
        val oppgave = Oppgave.Builder().setOppgaveId(oppgaveID).build()
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) } returns Optional.of(oppgave)
        justRun { oppgaveService.oppdaterOppgave(oppgaveID, fagsak.type, fagsak.tema, any(), any()) }

        fagsakEventListener.fagsakEndret(fagsakEndretAvSaksbehandler)

        verify { oppgaveService.oppdaterOppgave(oppgaveID, fagsak.type, fagsak.tema, any(), any())}
    }

    @Test
    fun `fagsakEndret - oppgave finnes ikke, oppgave opprettes`() {
        val fagsak = Fagsak().apply { saksnummer = SAKSNUMMER }
        val fagsakEndretAvSaksbehandler = FagsakEndretAvSaksbehandler(fagsak.saksnummer)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) } returns Optional.empty()
        justRun { oppgaveService.opprettOppgaveForSak(SAKSNUMMER) }

        fagsakEventListener.fagsakEndret(fagsakEndretAvSaksbehandler)

        verify { oppgaveService.opprettOppgaveForSak(SAKSNUMMER) }
    }
}
