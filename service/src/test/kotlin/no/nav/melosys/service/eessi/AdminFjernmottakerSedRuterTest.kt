package no.nav.melosys.service.eessi

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
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
class AdminFjernmottakerSedRuterTest {

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
    }

    private fun lagMelosysEessiMelding(x006NavErFjernet: Boolean = false) = MelosysEessiMelding(
        aktoerId = "12312412",
        rinaSaksnummer = "143141",
        journalpostId = "test-journalpost-id",
        x006NavErFjernet = x006NavErFjernet
    )

    private fun lagProsessinstans(melding: MelosysEessiMelding) =
        Prosessinstans.forTest { medData(ProsessDataKey.EESSI_MELDING, melding) }

    @Test
    fun `rutSedTilBehandling arkivsaksIdErNull opprettJournalFøringsOppgave`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melding.journalpostId!!,
                melding.aktoerId!!
            )
        }
    }

    @Test
    fun `rutSedTilBehandling finnesIngenTilhørendeFagsak opprettesJfrOppgave`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.empty()

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melding.journalpostId!!,
                melding.aktoerId!!
            )
        }
    }

    @Test
    fun `rutSedTilBehandling erIkkeX006MottakerPåÅpenA003 blirIkkeAvsluttetEllerSattTilAnnullert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)
        val melding = lagMelosysEessiMelding(x006NavErFjernet = false)
        val prosessinstans = lagProsessinstans(melding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerErIkkeTilstedePåSed opprettJournalFøringsProsess`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerPåÅpenA003 blirAvsluttetOgSattTilAnnullert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING)
        val melding = lagMelosysEessiMelding(x006NavErFjernet = true)
        val prosessinstans = lagProsessinstans(melding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        val behandlingsresultat = lagBehandlingsresultat(sistAktiveBehandling, medlPeriodeID = 20L)

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(20L) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling erX006MottakerPåAvsluttetBehandling oppdaterStatusPåFagsakTilAnnulert`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET)
        val melding = lagMelosysEessiMelding(x006NavErFjernet = true)
        val prosessinstans = lagProsessinstans(melding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        val behandlingsresultat = lagBehandlingsresultat(sistAktiveBehandling, medlPeriodeID = 20L)

        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(20L) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektAktiv behandlingsstausVURDER_DOKUMENT`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING)
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { behandlingService.endreStatus(sistAktiveBehandling.id, Behandlingsstatus.VURDER_DOKUMENT) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektIkkeAktiv journalføringsOppgaveLages`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        every {
            fagsakService.finnFagsakFraArkivsakID(arkivsakID)
        } returns Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET))

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melding.journalpostId!!,
                melding.aktoerId!!
            )
        }
    }

    private fun lagBehandlingsresultat(behandling: no.nav.melosys.domain.Behandling, medlPeriodeID: Long) =
        Behandlingsresultat.forTest {
            this.behandling = behandling
            anmodningsperiode { this.medlPeriodeID = medlPeriodeID }
        }

    private fun lagFagsak(behandlingstema: Behandlingstema, behandlingsstatus: Behandlingsstatus) = Fagsak.forTest {
        behandling {
            id = generateBehandlingID()
            tema = behandlingstema
            status = behandlingsstatus
        }
    }
}
