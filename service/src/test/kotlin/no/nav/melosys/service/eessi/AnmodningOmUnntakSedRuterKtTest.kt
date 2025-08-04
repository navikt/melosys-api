package no.nav.melosys.service.eessi

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.eessi.ruting.AnmodningOmUnntakSedRuter
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class AnmodningOmUnntakSedRuterKtTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var anmodningOmUnntakSedRuter: AnmodningOmUnntakSedRuter

    private val AKTØR_ID = "13412"
    private val GSAK_SAKSNUMMER = 132L
    private val NÅ = LocalDate.now()
    private val NESTE_ÅR = LocalDate.now().plusYears(1)

    @BeforeEach
    fun setup() {
        anmodningOmUnntakSedRuter = AnmodningOmUnntakSedRuter(prosessinstansService, fagsakService, behandlingsresultatService)
    }

    @Test
    fun `finnSakOgBestemRuting gsakSaksnummerErNull NySak`() {
        val prosessinstans = Prosessinstans()
        val melosysEessiMelding = MelosysEessiMelding().apply {
            aktoerId = AKTØR_ID
        }
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, null)
        verify {
            prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(
                melosysEessiMelding,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererPeriodeEndret nyBehandling`() {
        val fagsak = opprettFagsak()
        val prosessinstans = Prosessinstans()
        val melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        prosessinstans.behandling = fagsak.behandlinger[0]

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns opprettBehandlingsresultatMedLovvalgsperiode(
            NÅ,
            NESTE_ÅR.plusDays(1)
        )
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)
        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(
                melosysEessiMelding,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererPeriodeIkkeEndret ikkeNyBehandling`() {
        val fagsak = opprettFagsak()
        val prosessinstans = Prosessinstans()
        val melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        prosessinstans.behandling = fagsak.behandlinger[0]

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns opprettBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR)
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)
        verify(exactly = 0) {
            prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(any(), any())
        }
        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(
                fagsak.behandlinger[0],
                melosysEessiMelding
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererIkke nySak`() {
        val prosessinstans = Prosessinstans()
        val melosysEessiMelding = MelosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID)

        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.empty()
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)
        verify {
            prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(
                melosysEessiMelding,
                AKTØR_ID
            )
        }
    }

    private fun opprettBehandlingsresultatMedLovvalgsperiode(fom: LocalDate, tom: LocalDate): Behandlingsresultat {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.SE
            this.fom = fom
            this.tom = tom
        }

        return Behandlingsresultat().apply {
            lovvalgsperioder.add(lovvalgsperiode)
        }
    }

    private fun opprettFagsak(): Fagsak {
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.OPPRETTET)
            .build()

        val fagsak = FagsakTestFactory.lagFagsak()
        behandling.fagsak = fagsak
        fagsak.leggTilBehandling(behandling)
        return fagsak
    }

    private fun opprettMelosysEessiMelding(fom: LocalDate, tom: LocalDate): MelosysEessiMelding {
        return MelosysEessiMelding().apply {
            aktoerId = AKTØR_ID
            artikkel = "12_1"
            dokumentId = "123321"
            journalpostId = "j123"
            lovvalgsland = "SE"

            val periode = Periode().apply {
                this.fom = fom
                this.tom = tom
            }
            this.periode = periode

            val statsborgerskap = Statsborgerskap("SE")

            rinaSaksnummer = "r123"
            sedId = "s123"
            this.statsborgerskap = Collections.singletonList(statsborgerskap)
            sedType = "A001"
            bucType = "LA_BUC_01"
        }
    }
}
