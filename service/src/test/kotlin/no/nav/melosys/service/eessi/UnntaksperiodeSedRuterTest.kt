package no.nav.melosys.service.eessi

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.eessi.ruting.UnntaksperiodeSedRuter
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class UnntaksperiodeSedRuterTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var unntaksperiodeSedRuter: UnntaksperiodeSedRuter

    @BeforeEach
    fun setup() {
        unntaksperiodeSedRuter = UnntaksperiodeSedRuter(prosessinstansService, fagsakService, behandlingsresultatService)
    }

    @Test
    fun `finnSakOgBestemRuting nySak verifiserResultatNySak`() {
        val prosessinstans = lagProsessinstans(LocalDate.now(), LocalDate.now().plusYears(1), "SE")


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify {
            prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(
                any<MelosysEessiMelding>(),
                eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING),
                eq(AKTØR_ID)
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting oppdatertSedPåEksisterendeSakIkkeEndretPeriode skalIkkeBehandles`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(fom, tom, "SE")
        val behandlingsresultat = lagBehandlingsresultat(fom, tom, Land_iso2.SE)
        val fagsak = lagFagsak()

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(any()) } returns Optional.of(fagsak)


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(
                eq(fagsak.hentSistAktivBehandlingIkkeÅrsavregning()),
                any<MelosysEessiMelding>()
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting oppdatertSedPåEksisterendeSakIkkeEndretIngenLovvalgsLand skalIkkeBehandles`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(fom, tom, null)
        val behandlingsresultat = lagBehandlingsresultat(fom, tom, null)
        val fagsak = lagFagsak()

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(any()) } returns Optional.of(fagsak)


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(
                eq(fagsak.hentSistAktivBehandlingIkkeÅrsavregning()),
                any<MelosysEessiMelding>()
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting oppdatertSedPåEksisterendeSakErEndretLovvalgsLandForPeriode skalBehandles`() {
        val arkivsakID = 12321L
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(fom, tom, "SE")
        val behandlingsresultat = lagBehandlingsresultat(fom, tom, Land_iso2.IS)
        val fagsak = lagFagsak()

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(any()) } returns Optional.of(fagsak)


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)


        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingUnntaksregistrering(
                any<MelosysEessiMelding>(),
                eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING),
                eq(arkivsakID)
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting oppdatertSedPåEksisterendeSakErEndretLovvalgsLandForPeriodeFraNull skalBehandles`() {
        val arkivsakID = 12321L
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(1)
        val prosessinstans = lagProsessinstans(fom, tom, "SE")
        val behandlingsresultat = lagBehandlingsresultat(fom, tom, null)
        val fagsak = lagFagsak()

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(any()) } returns Optional.of(fagsak)


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)


        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingUnntaksregistrering(
                any<MelosysEessiMelding>(),
                eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING),
                eq(arkivsakID)
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting oppdatertSedPåEksisterendeSakErEndretPeriode skalBehandles`() {
        val arkivsakID = 12321L
        val fom = LocalDate.now()
        val tom: LocalDate? = null
        val prosessinstans = lagProsessinstans(fom, tom, "SE")
        val behandlingsresultat = lagBehandlingsresultat(fom.plusMonths(1), LocalDate.now().plusYears(2), null)
        val fagsak = lagFagsak()

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(any()) } returns Optional.of(fagsak)


        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID)


        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingUnntaksregistrering(
                any<MelosysEessiMelding>(),
                eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING),
                eq(arkivsakID)
            )
        }
    }

    private fun lagFagsak() = Fagsak.forTest {
        status = Saksstatuser.OPPRETTET
        behandling {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }
    }

    private fun lagBehandlingsresultat(fom: LocalDate, tom: LocalDate?, lovvalgsland: Land_iso2?) =
        Behandlingsresultat.forTest {
            lovvalgsperiode {
                this.fom = fom
                this.tom = tom
                this.lovvalgsland = lovvalgsland
            }
        }

    private fun lagProsessinstans(fom: LocalDate, tom: LocalDate?, lovvalgsLand: String?) = Prosessinstans.forTest {
        medData(ProsessDataKey.EESSI_MELDING, lagMelosysEessiMelding(fom, tom, lovvalgsLand))
    }

    private fun lagMelosysEessiMelding(fom: LocalDate, tom: LocalDate?, lovvalgsLand: String?) = MelosysEessiMelding(
        aktoerId = AKTØR_ID,
        artikkel = "12_1",
        dokumentId = "123321",
        journalpostId = "j123",
        lovvalgsland = lovvalgsLand,
        periode = Periode(fom = fom, tom = tom),
        rinaSaksnummer = "r123",
        sedId = "s123",
        statsborgerskap = Collections.singletonList(Statsborgerskap("SE")),
        sedType = "A009",
        bucType = "LA_BUC_04"
    )

    companion object {
        private const val AKTØR_ID = "143455432"
    }
}
