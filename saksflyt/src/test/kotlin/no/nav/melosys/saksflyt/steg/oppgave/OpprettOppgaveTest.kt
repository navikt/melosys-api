package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class OpprettOppgaveTest {
    @Mock
    private val oppgaveService: OppgaveService? = null
    private var opprettOppgave: OpprettOppgave? = null
    @BeforeEach
    fun setUp() {
        opprettOppgave = OpprettOppgave(oppgaveService!!)
    }

    @Test
    fun utfoerSteg_nySak_sendForvaltningsmelding() {
        val journalpostID = "142342343"
        val saksbehandler = "meg!"
        val fagsak = builder().medBruker().build()
        val behandling = Behandling()
        behandling.id = 243L
        behandling.initierendeJournalpostId = journalpostID
        behandling.fagsak = fagsak
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true)
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        opprettOppgave!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(
            behandling,
            journalpostID,
            FagsakTestFactory.BRUKER_AKTØR_ID,
            saksbehandler,
            null
        )
    }

    @Test
    fun utfoerSteg_nyOppgave_virksomhet() {
        val journalpostID = "142342343"
        val saksbehandler = "meg!"
        val fagsak = builder().medVirksomhet().build()
        val behandling = Behandling()
        behandling.id = 243L
        behandling.initierendeJournalpostId = journalpostID
        behandling.fagsak = fagsak
        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true)
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        opprettOppgave!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(
            behandling,
            journalpostID,
            null,
            saksbehandler,
            FagsakTestFactory.ORGNR
        )
    }
}
