package no.nav.melosys.service.eessi

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.domain.sedDokument
import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedInformasjon
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class AdminInnvalideringSedRuterTest {

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
    lateinit var eessiService: EessiService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    private lateinit var adminInnvalideringSedRuter: AdminInnvalideringSedRuter

    private val behandlingID = 111L
    private val arkivsakID = 123321L
    private val rinaSaksnummer = "1233333"
    private val sedID = "2414"

    @BeforeEach
    fun setup() {
        adminInnvalideringSedRuter = AdminInnvalideringSedRuter(
            fagsakService,
            prosessinstansService,
            behandlingsresultatService,
            medlPeriodeService,
            oppgaveService,
            eessiService,
            behandlingService
        )
    }

    private fun lagMelosysEessiMelding() = MelosysEessiMelding(
        aktoerId = "12312412",
        rinaSaksnummer = "143141",
        journalpostId = "1111111"
    )

    private fun lagProsessinstans(melding: MelosysEessiMelding = lagMelosysEessiMelding()) =
        Prosessinstans.forTest { medData(ProsessDataKey.EESSI_MELDING, melding) }

    @Test
    fun `gjelderSedTyper skal returnere collection med X008 når feature toggle er på`() {
        adminInnvalideringSedRuter.gjelderSedTyper().shouldContainExactly(SedType.X008)
    }

    @Test
    fun `rutSedTilBehandling skal opprette journalføringsoppgave når arkivsakId er null`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify { oppgaveService.opprettJournalføringsoppgave(melding.journalpostId!!, melding.aktoerId!!) }
    }

    @Test
    fun `rutSedTilBehandling skal opprette journalføringsoppgave når ingen tilhørende fagsak finnes`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.empty()

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { oppgaveService.opprettJournalføringsoppgave(melding.journalpostId!!, melding.aktoerId!!) }
    }

    @Test
    fun `rutSedTilBehandling skal sette behandlingsstatus til VURDER_DOKUMENT når tilhørende fagsak finnes og behandling er Norge utpekt og aktiv`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING)
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { behandlingService.endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melding) }
    }

    @Test
    fun `rutSedTilBehandling skal lage journalføringsoppgave når tilhørende fagsak finnes og behandling er Norge utpekt men ikke aktiv`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        every {
            fagsakService.finnFagsakFraArkivsakID(arkivsakID)
        } returns Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET))

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { oppgaveService.opprettJournalføringsoppgave(melding.journalpostId!!, melding.aktoerId!!) }
    }

    @Test
    fun `rutSedTilBehandling skal oppdatere saksstatus til annullert og opphøre MEDL-periode når behandling er utland utpekt og avsluttet med MEDL-periode`() {
        val prosessinstans = lagProsessinstans()
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET, medSedSaksopplysning = true)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        val behandlingsresultat = lagBehandlingsresultat(sistAktiveBehandling, medMedlperiode = true)

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("AVBRUTT")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(behandlingsresultat.hentLovvalgsperiode().hentMedlPeriodeID()) }
    }

    @Test
    fun `rutSedTilBehandling skal oppdatere saksstatus til annullert når behandling er utstasjonering og aktiv`() {
        val prosessinstans = lagProsessinstans()
        val fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingsstatus.UNDER_BEHANDLING, medSedSaksopplysning = true)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = sistAktiveBehandling
            anmodningsperiode { }
        }

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("AVBRUTT")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT) }
    }

    @Test
    fun `rutSedTilBehandling skal opprette behandlingsoppgave når behandling er unntak norsk trygd øvrig aktiv og SED ikke er annullert`() {
        val melding = lagMelosysEessiMelding()
        val prosessinstans = lagProsessinstans(melding)
        val fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingsstatus.UNDER_BEHANDLING, medSedSaksopplysning = true)

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("ÅPEN")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                any<Behandling>(),
                eq(melding.journalpostId!!),
                eq(melding.aktoerId!!),
                isNull(),
                isNull()
            )
        }
    }

    private fun lagBehandlingsresultat(behandling: Behandling, medMedlperiode: Boolean) =
        Behandlingsresultat.forTest {
            this.behandling = behandling
            if (medMedlperiode) {
                lovvalgsperiode { medlPeriodeID = 123L }
            } else {
                lovvalgsperiode { }
            }
        }

    private fun lagBucInformasjon(status: String): List<BucInformasjon> = listOf(
        BucInformasjon(
            rinaSaksnummer,
            true,
            "LA_BUC_04",
            LocalDate.now(),
            setOf(),
            listOf(SedInformasjon(rinaSaksnummer, sedID, LocalDate.now(), LocalDate.now(), null, status, null))
        )
    )

    private fun lagFagsak(
        behandlingstema: Behandlingstema,
        behandlingsstatus: Behandlingsstatus,
        medSedSaksopplysning: Boolean = false
    ): Fagsak {
        val behandling = Behandling.forTest {
            id = behandlingID
            tema = behandlingstema
            status = behandlingsstatus
            if (medSedSaksopplysning) {
                saksopplysning {
                    type = SaksopplysningType.SEDOPPL
                    sedDokument {
                        rinaSaksnummer = this@AdminInnvalideringSedRuterTest.rinaSaksnummer
                        rinaDokumentID = sedID
                    }
                }
            }
        }
        return Fagsak.forTest { behandlinger(behandling) }
    }
}
