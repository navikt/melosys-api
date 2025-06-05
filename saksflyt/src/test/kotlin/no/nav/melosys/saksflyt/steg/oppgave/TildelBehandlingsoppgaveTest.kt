package no.nav.melosys.saksflyt.steg.oppgave

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
internal class TildelBehandlingsoppgaveTest {
    @MockK
    private lateinit var oppgaveService: OppgaveService

    private lateinit var tildelBehandlingsoppgave: TildelBehandlingsoppgave
    private lateinit var prosessinstans: Prosessinstans
    @BeforeEach
    fun setUp() {
        tildelBehandlingsoppgave = TildelBehandlingsoppgave(oppgaveService)
        prosessinstans = Prosessinstans().apply {
            setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER)
            setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        }
        every {oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER)} returns
            Optional.of(Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build())
        every {oppgaveService.tildelOppgave(any(), any())} returns Unit
    }

    @Test
    fun utfør_finnerOppgave_forventTildelingAvOppgave() {
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true)
        every { oppgaveService.finnÅpneBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) } returns listOf(Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build())

        tildelBehandlingsoppgave.utfør(prosessinstans)

        verify {oppgaveService.tildelOppgave(OPPGAVE_ID, SAKSBEHANDLER)}
    }

    @Test
    fun utfør_finnerOppgave_forventTildelingAvFlereOppgaver() {
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true)
        every { oppgaveService.finnÅpneBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) } returns listOf(Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build(), Oppgave.Builder().setOppgaveId(OPPGAVE_ID2).build())

        tildelBehandlingsoppgave.utfør(prosessinstans)

        verify {oppgaveService.tildelOppgave(OPPGAVE_ID, SAKSBEHANDLER)}
        verify {oppgaveService.tildelOppgave(OPPGAVE_ID2, SAKSBEHANDLER)}
    }

    companion object {
        private const val SAKSBEHANDLER = "Z998877"
        private const val SAKSNUMMER = "MEL-1234"
        private const val OPPGAVE_ID = "123123"
        private const val OPPGAVE_ID2 = "123122"
    }
}
