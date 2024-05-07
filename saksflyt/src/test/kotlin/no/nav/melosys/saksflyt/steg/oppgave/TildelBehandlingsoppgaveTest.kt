package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class TildelBehandlingsoppgaveTest {
    @Mock
    private val oppgaveService: OppgaveService? = null
    private var tildelBehandlingsoppgave: TildelBehandlingsoppgave? = null
    private var prosessinstans: Prosessinstans? = null
    @BeforeEach
    fun setUp() {
        tildelBehandlingsoppgave = TildelBehandlingsoppgave(oppgaveService!!)
        prosessinstans = Prosessinstans()
        prosessinstans!!.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER)
        prosessinstans!!.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        val oppgaveBuilder = Oppgave.Builder()
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID)
        val oppgave = oppgaveBuilder.build()
        Mockito.`when`(oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER))
            .thenReturn(Optional.of(oppgave))
    }

    @Test
    fun utfør_finnerOppgave_forventTildelingAvOppgave() {
        prosessinstans!!.setData(ProsessDataKey.SKAL_TILORDNES, true)
        tildelBehandlingsoppgave!!.utfør(prosessinstans!!)
        Mockito.verify(oppgaveService).tildelOppgave(OPPGAVE_ID, SAKSBEHANDLER)
    }

    companion object {
        private const val SAKSBEHANDLER = "Z998877"
        private const val SAKSNUMMER = "MEL-1234"
        private const val OPPGAVE_ID = "123123"
    }
}
