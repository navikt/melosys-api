package no.nav.melosys.saksflyt.steg.oppgave

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.prosessinstansForTest
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
        every {oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any<Behandling>(), any<String>(), any<String>(), any<String>(), any<String>())} returns Unit
    }

    @Test
    fun utfoerSteg_nySak_sendForvaltningsmelding() {
        val journalpostID = "142342343"
        val saksbehandler = "meg!"
        val fagsak = builder().medBruker().build()
        val prosessinstans = prosessinstansForTest {
            behandling {
                id = 243L
                initierendeJournalpostId = journalpostID
                this.fagsak = fagsak
            }
            medData(ProsessDataKey.SKAL_TILORDNES, true)
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                Behandling.forTest {
                    id = 243L
                    initierendeJournalpostId = journalpostID
                    this.fagsak = fagsak
                },
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
        val prosessinstans = prosessinstansForTest {
            behandling {
                id = 243L
                initierendeJournalpostId = journalpostID
                this.fagsak = fagsak
            }
            medData(ProsessDataKey.SKAL_TILORDNES, true)
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                Behandling.forTest {
                    id = 243L
                    initierendeJournalpostId = journalpostID
                    this.fagsak = fagsak
                },
                journalpostID,
                null,
                saksbehandler,
                FagsakTestFactory.ORGNR
            )
        }
    }
}
