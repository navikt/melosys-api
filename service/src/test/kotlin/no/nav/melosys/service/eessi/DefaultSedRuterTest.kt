package no.nav.melosys.service.eessi

import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class DefaultSedRuterTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    private lateinit var defaultSedRuter: DefaultSedRuter

    private val oppgaveFactory = OppgaveFactory()

    @BeforeEach
    fun setup() {
        defaultSedRuter = DefaultSedRuter(prosessinstansService, fagsakService, behandlingService, oppgaveService)
    }

    @Test
    fun `bestemManuellBehandling saksnummerFinnesIkkeErNySedINyBehandling nesteStegOppretJfrOppg`() {
        val prosessinstans = Prosessinstans.forTest()
        val melosysEessiMelding = hentMelosysEessiMelding(SedType.A005)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        defaultSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId!!,
                melosysEessiMelding.aktoerId!!
            )
        }
    }

    @Test
    fun `bestemManuellBehandling X009PurringSaksnummerOgFagsakEksisterer oppdatererPrioritet`() {
        val oppgaveId = "333"
        val prosessinstans = Prosessinstans.forTest()
        val melosysEessiMelding = hentMelosysEessiMelding(SedType.X009)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        val oppgave = Oppgave.Builder().setOppgaveId(oppgaveId).build()
        val fagsak = hentFagsak()

        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) } returns Optional.of(oppgave)

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        prosessinstans.behandling shouldNotBe null
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(fagsak.finnAktivBehandlingIkkeÅrsavregning(), melosysEessiMelding) }
        verify { behandlingService.endreStatus(any<Long>(), eq(Behandlingsstatus.VURDER_DOKUMENT)) }
        verify { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER) }
        verify { oppgaveService.oppdaterOppgave(eq(oppgaveId), any<OppgaveOppdatering>()) }
    }

    @ParameterizedTest
    @EnumSource(value = SedType::class, names = ["A012", "X001", "X007", "X005"])
    fun `rutSedTilBehandling SedTyperSaksnummerOgFagsakEksistererStatusMidlertidigLovvalgsbeslutning ikkeOppdaterStatusEllerOppgave`(sedType: SedType) {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER)
        val melosysEessiMelding = hentMelosysEessiMelding(sedType)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        val fagsak = hentFagsak()
        fagsak.hentSistOppdatertBehandlingIkkeÅrsavregning().status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING

        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        prosessinstans.behandling shouldNotBe null
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(fagsak.hentSistAktivBehandlingIkkeÅrsavregning(), melosysEessiMelding) }
        verify(exactly = 0) { behandlingService.endreStatus(any<Long>(), any()) }
        verify(exactly = 0) { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(any()) }
        verify(exactly = 0) { oppgaveService.opprettJournalføringsoppgave(any(), any()) }
    }

    @Test
    fun `bestemManuellBehandling behandlingOpprettetOgSkalBehandleSED opprettOppgave`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER)
        val melosysEessiMelding = hentMelosysEessiMelding(SedType.A004)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        val fagsak = hentFagsak()
        val behandling = fagsak.finnAktivBehandlingIkkeÅrsavregning()
        behandling?.type = Behandlingstyper.HENVENDELSE
        behandling?.status = Behandlingsstatus.OPPRETTET
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)
        every { oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(any()) } returns Optional.empty()
        every { oppgaveService.lagBehandlingsoppgave(any()) } returns oppgaveFactory.lagBehandlingsoppgave(
            behandling!!,
            LocalDate.now(),
            behandling::hentSedDokument
        )
        every { oppgaveService.opprettOppgave(any<Oppgave>()) } returns "test-oppgave-id"
        every { oppgaveService.oppdaterOppgave(any(), any<OppgaveOppdatering>()) } just Runs
        every { prosessinstansService.opprettProsessinstansSedJournalføring(any(), any()) } returns UUID.randomUUID()
        every { behandlingService.endreStatus(any<Long>(), any()) } returns Unit

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify { behandlingService.endreStatus(any<Long>(), eq(Behandlingsstatus.VURDER_DOKUMENT)) }
        verify { oppgaveService.opprettOppgave(any<Oppgave>()) }
        verify { oppgaveService.oppdaterOppgave(any(), any<OppgaveOppdatering>()) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(fagsak.hentSistAktivBehandlingIkkeÅrsavregning(), melosysEessiMelding) }
    }

    @Test
    fun `bestemManuellBehandling behandlingAvsluttet opprettJournalforingsOppgave`() {
        val prosessinstans = Prosessinstans.forTest()
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER)
        val melosysEessiMelding = hentMelosysEessiMelding(SedType.A004)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        val fagsak = hentFagsak()
        val behandling = fagsak.finnAktivBehandlingIkkeÅrsavregning()
        behandling?.status = Behandlingsstatus.AVSLUTTET
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId!!,
                melosysEessiMelding.aktoerId!!
            )
        }
        verify(exactly = 0) { oppgaveService.opprettOppgave(any<Oppgave>()) }
        verify(exactly = 0) { behandlingService.endreStatus(any<Long>(), any()) }
        verify(exactly = 0) { prosessinstansService.opprettProsessinstansSedJournalføring(any(), any()) }
    }

    private fun hentMelosysEessiMelding(sedType: SedType): MelosysEessiMelding {
        return MelosysEessiMelding().apply {
            this.sedType = sedType.name
            aktoerId = "12321321"
            journalpostId = "test-journalpost-id"
        }
    }

    private fun hentFagsak() = Fagsak.forTest {
        behandling {
            id = 1L
            status = Behandlingsstatus.UNDER_BEHANDLING
            tema = Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET
            type = Behandlingstyper.FØRSTEGANG
            saksopplysninger.add(Saksopplysning().apply {
                type = SaksopplysningType.SEDOPPL
                dokument = SedDokument().apply {
                    sedType = SedType.A003
                }
            })
        }
    }

    companion object {
        private const val GSAK_SAKSNUMMER = 123L
    }
}
