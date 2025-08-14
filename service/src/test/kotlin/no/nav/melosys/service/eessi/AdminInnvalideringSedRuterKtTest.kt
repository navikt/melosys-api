package no.nav.melosys.service.eessi

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.dokument.sed.SedDokument
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
class AdminInnvalideringSedRuterKtTest {

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
    private val prosessinstans = Prosessinstans()
    private val melosysEessiMelding = MelosysEessiMelding()
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

        melosysEessiMelding.apply {
            aktoerId = "12312412"
            rinaSaksnummer = "143141"
            journalpostId = "1111111"
        }
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
    }

    @Test
    fun `gjelderSedTyper skal returnere collection med X008 når feature toggle er på`() {
        adminInnvalideringSedRuter.gjelderSedTyper().shouldContainExactly(SedType.X008)
    }

    @Test
    fun `rutSedTilBehandling skal opprette journalføringsoppgave når arkivsakId er null`() {
        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, null)
        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `rutSedTilBehandling skal opprette journalføringsoppgave når ingen tilhørende fagsak finnes`() {
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.empty()
        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)
        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `rutSedTilBehandling skal sette behandlingsstatus til VURDER_DOKUMENT når tilhørende fagsak finnes og behandling er Norge utpekt og aktiv`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING)
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { behandlingService.endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding) }
    }

    @Test
    fun `rutSedTilBehandling skal lage journalføringsoppgave når tilhørende fagsak finnes og behandling er Norge utpekt men ikke aktiv`() {
        every {
            fagsakService.finnFagsakFraArkivsakID(arkivsakID)
        } returns Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET))

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding.journalpostId,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `rutSedTilBehandling skal oppdatere saksstatus til annullert og opphøre MEDL-periode når behandling er utland utpekt og avsluttet med MEDL-periode`() {
        val fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET)
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().saksopplysninger.add(lagSedDokument())
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        val behandlingsresultat = lagBehandlingsresultat(true)
        behandlingsresultat.behandling = sistAktiveBehandling

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("AVBRUTT")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) }
        verify { medlPeriodeService.avvisPeriodeOpphørt(behandlingsresultat.hentLovvalgsperiode().medlPeriodeID) }
    }

    @Test
    fun `rutSedTilBehandling skal oppdatere saksstatus til annullert når behandling er utstasjonering og aktiv`() {
        val fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingsstatus.UNDER_BEHANDLING)
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().saksopplysninger.add(lagSedDokument())
        val sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning()

        val behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = sistAktiveBehandling
        }

        val periode = Anmodningsperiode()
        behandlingsresultat.anmodningsperioder.add(periode)

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("AVBRUTT")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT) }
    }

    @Test
    fun `rutSedTilBehandling skal opprette behandlingsoppgave når behandling er unntak norsk trygd øvrig aktiv og SED ikke er annullert`() {
        val fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingsstatus.UNDER_BEHANDLING)
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().saksopplysninger.add(lagSedDokument())

        every { eessiService.hentTilknyttedeBucer(arkivsakID, listOf()) } returns lagBucInformasjon("ÅPEN")
        every { fagsakService.finnFagsakFraArkivsakID(arkivsakID) } returns Optional.of(fagsak)

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)
        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                any<Behandling>(),
                eq(melosysEessiMelding.journalpostId),
                eq(melosysEessiMelding.aktoerId),
                isNull(),
                isNull()
            )
        }
    }

    private fun lagBehandlingsresultat(medMedlperiode: Boolean): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()

        val lovvalgsperiode = Lovvalgsperiode()
        if (medMedlperiode) {
            lovvalgsperiode.medlPeriodeID = 123L
        }

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        return behandlingsresultat
    }

    private fun lagBucInformasjon(status: String): List<BucInformasjon> {
        return listOf(
            BucInformasjon(
                rinaSaksnummer,
                true,
                "LA_BUC_04",
                LocalDate.now(),
                setOf(),
                listOf(SedInformasjon(rinaSaksnummer, sedID, null, null, null, status, null))
            )
        )
    }

    private fun lagSedDokument(): Saksopplysning {
        val sedDokument = SedDokument().apply {
            rinaSaksnummer = this@AdminInnvalideringSedRuterKtTest.rinaSaksnummer
            rinaDokumentID = sedID
        }

        return Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = sedDokument
        }
    }

    private fun lagBehandling(fagsak: Fagsak, behandlingstema: Behandlingstema, behandlingsstatus: Behandlingsstatus): Behandling {
        return Behandling.forTest {
            id = behandlingID
            tema = behandlingstema
            this.fagsak = fagsak
            status = behandlingsstatus
        }
    }

    private fun lagFagsak(behandlingstema: Behandlingstema, behandlingsstatus: Behandlingsstatus): Fagsak {
        val fagsak = Fagsak.forTest {}
        lagBehandling(fagsak, behandlingstema, behandlingsstatus)
        return fagsak
    }
}
