package no.nav.melosys.service.eessi.jobb

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.repository.KontrollresultatRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class FeilregistrerX100OppgaverJobbTest {
    @Mock
    private val prosessinstansRepository: ProsessinstansRepository? = null
    @Mock
    private val kontrollresultatRepository: KontrollresultatRepository? = null
    @Mock
    private val oppgaveService: OppgaveService? = null
    @Mock
    private val behandlingService: BehandlingService? = null

    private var feilregistrerX100OppgaverJobb: FeilregistrerX100OppgaverJobb? = null

    @BeforeEach
    fun setup() {
        feilregistrerX100OppgaverJobb = FeilregistrerX100OppgaverJobb(
            prosessinstansRepository!!, kontrollresultatRepository!!, oppgaveService!!, behandlingService!!
        )
    }

    @Test
    fun feilregistrerX100Behandlingsoppgaver() {
        val prosessMedX100 = lagProsessinstansMedEessiMelding()
        Mockito.`when`(prosessinstansRepository!!.findAllWithSedX100()).thenReturn(setOf(prosessMedX100))
        val oppgave = Oppgave.Builder().setOppgaveId(OPPGAVE_ID_X100).build()
        Mockito.`when`(oppgaveService!!.finnÅpenOppgaveMedFagsaksnummer("MEL-1"))
            .thenReturn(Optional.of(oppgave))

        feilregistrerX100OppgaverJobb!!.feilregistrerX100Behandlingsoppgaver()

        Mockito.verify(oppgaveService).feilregistrerOppgaver(setOf(oppgave))
        Mockito.verify(behandlingService!!).avsluttBehandling(anyLong())
    }

    @Test
    fun feilregistrerX100Journalføringsoppgaver() {
        val prosessMedX100 = lagProsessinstansMedEessiMelding()
        Mockito.`when`(prosessinstansRepository!!.findAllWithSedX100()).thenReturn(setOf(prosessMedX100))
        val oppgave = Oppgave.Builder().setOppgaveId(OPPGAVE_ID_X100).build()
        Mockito.`when`(oppgaveService!!.finnÅpneOppgaverMedJournalpostID(JOURNALPOST_ID_X_100))
            .thenReturn(listOf(oppgave))

        feilregistrerX100OppgaverJobb!!.feilregistrerX100Journalføringsoppgaver()

        Mockito.verify(oppgaveService).feilregistrerOppgaver(setOf(oppgave))
    }

    companion object {
        private const val JOURNALPOST_ID_X_100 = "journalpostID-X100"
        private const val OPPGAVE_ID_X100 = "X100"
        private fun lagProsessinstansMedEessiMelding(): Prosessinstans {
            val behandling = SaksbehandlingDataFactory.lagBehandling(
                SaksbehandlingDataFactory.lagFagsak("MEL-1")
            )
            behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            behandling.status = Behandlingsstatus.VURDER_DOKUMENT

            val prosessMedX100 = Prosessinstans()
            prosessMedX100.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID_X_100)
            prosessMedX100.setData(ProsessDataKey.EESSI_MELDING, lagEessiMeldingMedX100())
            prosessMedX100.behandling = behandling
            return prosessMedX100
        }

        private fun lagEessiMeldingMedX100(): String {
            return """
            {"sedId"\\:"sedId","rinaSaksnummer"\\:"rinaSaksnummer",
            "avsender"\\:{"avsenderID"\\:"AT\\:3100","landkode"\\:"AT"},"journalpostId"\\:"jpID","dokumentId"\\:null,
            "gsakSaksnummer"\\:gsakSaksnummer,"aktoerId"\\:"aktoerId","statsborgerskap"\\:[],"arbeidssteder"\\:[],
            "periode"\\:null,"lovvalgsland"\\:null,"artikkel"\\:null,"erEndring"\\:false,"midlertidigBestemmelse"\\:false,
            "x006NavErFjernet"\\:false,"ytterligereInformasjon"\\:null,"bucType"\\:"LA_BUC_02","sedType"\\:"X100",
            "sedVersjon"\\:"2","svarAnmodningUnntak"\\:null,"anmodningUnntak"\\:null}
            """
        }
    }
}
