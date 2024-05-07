package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.lagFagsak
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class GjenbrukOppgaveTest {
    @Mock
    private val oppgaveService: OppgaveService? = null

    @Captor
    private val oppgaveCaptor: ArgumentCaptor<Oppgave>? = null
    private var gjenbrukOppgave: GjenbrukOppgave? = null
    private val oppgaveFactory = OppgaveFactory()
    @BeforeEach
    fun setUp() {
        gjenbrukOppgave = GjenbrukOppgave(oppgaveService!!)
    }

    @Test
    fun gjenbrukOppgave_utfør_oppdatererOppgave() {
        val oppgaveID = "1234"
        val oppgaveBeskrivelse = "jeg beskriver oppgave"
        val eksisterendeOppgave = Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build()
        Mockito.`when`(oppgaveService!!.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave)
        val prosessinstans = lagProsessinstans(oppgaveID, false)
        Mockito.`when`(oppgaveService.lagBehandlingsoppgave(ArgumentMatchers.any()))
            .thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.behandling, LocalDate.now()) { null })
        gjenbrukOppgave!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).opprettOppgave(oppgaveCaptor!!.capture())
        Assertions.assertThat(oppgaveCaptor.value)
            .hasFieldOrPropertyWithValue("saksnummer", FagsakTestFactory.SAKSNUMMER)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.kode)
            .hasFieldOrPropertyWithValue("behandlingstype", null)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("aktørId", "123321")
    }

    @Test
    fun gjenbrukOppgave_utfør_oppdatererOppgave_virksomhet() {
        val oppgaveID = "1234"
        val oppgaveBeskrivelse = "jeg beskriver oppgave"
        val eksisterendeOppgave = Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build()
        Mockito.`when`(oppgaveService!!.hentOppgaveMedOppgaveID(oppgaveID)).thenReturn(eksisterendeOppgave)
        val prosessinstans = lagProsessinstans(oppgaveID, true)
        Mockito.`when`(oppgaveService.lagBehandlingsoppgave(ArgumentMatchers.any()))
            .thenReturn(oppgaveFactory.lagBehandlingsoppgave(prosessinstans.behandling, LocalDate.now()) { null })
        gjenbrukOppgave!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).opprettOppgave(oppgaveCaptor!!.capture())
        Assertions.assertThat(oppgaveCaptor.value)
            .hasFieldOrPropertyWithValue("saksnummer", FagsakTestFactory.SAKSNUMMER)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.kode)
            .hasFieldOrPropertyWithValue("behandlingstype", null)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("orgnr", "999999999")
    }

    companion object {
        private fun lagProsessinstans(oppgaveID: String, erForVirksomhet: Boolean): Prosessinstans {
            val fagsak = lagFagsak()
            val prosessinstans = Prosessinstans()
            if (erForVirksomhet) {
                val virksomhet = Aktoer()
                virksomhet.orgnr = "999999999"
                virksomhet.rolle = Aktoersroller.VIRKSOMHET
                fagsak.leggTilAktør(virksomhet)
            } else {
                val bruker = Aktoer()
                bruker.aktørId = "123321"
                bruker.rolle = Aktoersroller.BRUKER
                fagsak.leggTilAktør(bruker)
            }
            val behandling = Behandling()
            behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandling.type = Behandlingstyper.FØRSTEGANG
            behandling.fagsak = fagsak
            prosessinstans.behandling = behandling
            prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID)
            prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true)
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "Deg321")
            return prosessinstans
        }
    }
}
