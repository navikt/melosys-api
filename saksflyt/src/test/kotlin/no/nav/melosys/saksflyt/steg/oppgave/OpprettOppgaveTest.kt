package no.nav.melosys.saksflyt.steg.oppgave

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
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
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 243L
                initierendeJournalpostId = journalpostID
                fagsak { medBruker() }
            }
            medData(ProsessDataKey.SKAL_TILORDNES, true)
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                match { it.id == 243L && it.initierendeJournalpostId == journalpostID },
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
        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = 243L
                initierendeJournalpostId = journalpostID
                fagsak { medVirksomhet() }
            }
            medData(ProsessDataKey.SKAL_TILORDNES, true)
            medData(ProsessDataKey.SAKSBEHANDLER, saksbehandler)
        }

        opprettOppgave.utfør(prosessinstans)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                match { it.id == 243L && it.initierendeJournalpostId == journalpostID },
                journalpostID,
                null,
                saksbehandler,
                FagsakTestFactory.ORGNR
            )
        }
    }
}
