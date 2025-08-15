package no.nav.melosys.service.eessi

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.eessi.ruting.AdminFjernmottakerSedRuter
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class AdminFjernmottakerSedRuterKtTest {

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var medlPeriodeService: MedlPeriodeService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    private lateinit var adminFjernmottakerSedRuter: AdminFjernmottakerSedRuter

    private fun generateBehandlingID() = System.nanoTime()
    private val arkivsakID = 123321L
    private val prosessinstans = Prosessinstans()
    private val melosysEessiMelding = MelosysEessiMelding()

    @BeforeEach
    fun setup() {
        adminFjernmottakerSedRuter = AdminFjernmottakerSedRuter(
            fagsakService,
            prosessinstansService,
            oppgaveService,
            behandlingsresultatService,
            medlPeriodeService,
            behandlingService
        )

        melosysEessiMelding.apply {
            aktoerId = "12312412"
            rinaSaksnummer = "143141"
            journalpostId = "test-journalpost-id"
        }
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
    }

    @Test
    fun `rutSedTilBehandling arkivsaksIdErNull opprettJournalFøringsOppgave`() {
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, null)
        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `rutSedTilBehandling finnesIngenTilhørendeFagsak opprettesJfrOppgave`() {
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.empty()
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)
        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `rutSedTilBehandling erIkkeX006MottakerPåÅpenA003 blirIkkeAvsluttetEllerSattTilAnnullert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)
        melosysEessiMelding.setX006NavErFjernet(false)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerErIkkeTilstedePåSed opprettJournalFøringsProsess`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)
        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerPåÅpenA003 blirAvsluttetOgSattTilAnnullert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)
        melosysEessiMelding.setX006NavErFjernet(true)

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = sistAktiveBehandling
        }

        val anmodningsperiode = Anmodningsperiode().apply {
            medlPeriodeID = 20L
        }
        behandlingsresultat.anmodningsperioder = setOf(anmodningsperiode)

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(behandlingsresultat.hentAnmodningsperiode().medlPeriodeID) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerPåAvsluttetBehandling oppdaterStatusPåFagsakTilAnnulert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET)
        melosysEessiMelding.setX006NavErFjernet(true)

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = sistAktiveBehandling
        }

        val anmodningsperiode = Anmodningsperiode().apply {
            medlPeriodeID = 20L
        }
        behandlingsresultat.anmodningsperioder = setOf(anmodningsperiode)

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(anmodningsperiode.medlPeriodeID) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektAktiv behandlingsstausVURDER_DOKUMENT`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING)
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { behandlingService.endreStatus(sistAktiveBehandling.id, Behandlingsstatus.VURDER_DOKUMENT) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektIkkeAktiv journalføringsOppgaveLages`() {
        every {
            fagsakService.finnFagsakFraArkivsakID(arkivsakID)
        } returns Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET))

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    private fun lagFagsak(behandlingstema: Behandlingstema, behandlingsstatus: Behandlingsstatus) = Fagsak.forTest {
        behandling {
            id = generateBehandlingID()
            tema = behandlingstema
            status = behandlingsstatus
        }
    }
}
