package no.nav.melosys.saksflyt.steg.oppgave

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpprettOppgaveTest {
    @MockK
    private lateinit var oppgaveService: OppgaveService

    private lateinit var opprettOppgave: OpprettOppgave

    @BeforeEach
    fun setUp() {
        opprettOppgave = OpprettOppgave(oppgaveService)
        every {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                any<Behandling>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>()
            )
        } returns Unit
    }

    @Test
    fun utfoerSteg_nySak_sendForvaltningsmelding() {
        val journalpostID = "142342343"
        val saksbehandler = "meg!"
        val fagsak = builder().medBruker().build()
        val behandling = Behandling().apply {
            id = 243L
            initierendeJournalpostId = journalpostID
            this.fagsak = fagsak
        }
        val prosessinstans = Prosessinstans().apply {
            this.behandling = behandling
            setData(ProsessDataKey.SKAL_TILORDNES, true)
            setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                behandling,
                journalpostID,
                FagsakTestFactory.BRUKER_AKTØR_ID,
                saksbehandler,
                null
            )
        }
    }

    @Test
    fun utfoerSteg_nyOppgave_virksomhet() {
        val journalpostID = "142342343"
        val saksbehandler = "meg!"
        val fagsak = builder().medVirksomhet().build()
        val behandling = Behandling().apply {
            id = 243L
            initierendeJournalpostId = journalpostID
            this.fagsak = fagsak
        }
        val prosessinstans = Prosessinstans().apply {
            this.behandling = behandling
            setData(ProsessDataKey.SKAL_TILORDNES, true)
            setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                behandling,
                journalpostID,
                null,
                saksbehandler,
                FagsakTestFactory.ORGNR
            )
        }
    }


    @Test
    fun `skal ikke lage oppgave når opprett årsavregning ikke lager en behandling`() {
        val saksbehandler = "meg!"
        val prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.SKAL_TILORDNES, true)
            setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
            setData(ProsessDataKey.FAKTURANUMMER, "mel-123")
            setData(ProsessDataKey.ÅRSAVREGNING_STEG_KJØRT_UTEN_BEHANDLING, true)
        }

        opprettOppgave.utfør(prosessinstans)

        verify(exactly = 0) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any())
        }
    }
}
