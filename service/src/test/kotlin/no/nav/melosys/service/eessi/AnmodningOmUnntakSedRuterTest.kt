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
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.eessi.ruting.AnmodningOmUnntakSedRuter
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class AnmodningOmUnntakSedRuterTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var anmodningOmUnntakSedRuter: AnmodningOmUnntakSedRuter

    @BeforeEach
    fun setup() {
        anmodningOmUnntakSedRuter = AnmodningOmUnntakSedRuter(prosessinstansService, fagsakService, behandlingsresultatService)
    }

    @Test
    fun `finnSakOgBestemRuting gsakSaksnummerErNull NySak`() {
        val prosessinstans = Prosessinstans.forTest()
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
        val prosessinstans = Prosessinstans.forTest()
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
        val prosessinstans = Prosessinstans.forTest()
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
        val prosessinstans = Prosessinstans.forTest()
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

    private fun opprettBehandlingsresultatMedLovvalgsperiode(fom: LocalDate, tom: LocalDate): Behandlingsresultat =
        Behandlingsresultat().apply {
            lovvalgsperioder.add(
                Lovvalgsperiode().apply {
                    lovvalgsland = Land_iso2.SE
                    this.fom = fom
                    this.tom = tom
                }
            )
        }

    private fun opprettFagsak(): Fagsak =
        Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.OPPRETTET
            fagsak { }
        }.fagsak

    private fun opprettMelosysEessiMelding(fom: LocalDate, tom: LocalDate): MelosysEessiMelding =
        MelosysEessiMelding().apply {
            aktoerId = AKTØR_ID
            artikkel = "12_1"
            dokumentId = "123321"
            journalpostId = "j123"
            lovvalgsland = "SE"

            periode = Periode(fom, tom)
            rinaSaksnummer = "r123"
            sedId = "s123"
            statsborgerskap = Collections.singletonList(Statsborgerskap("SE"))
            sedType = "A001"
            bucType = "LA_BUC_01"
        }

    companion object {
        const val AKTØR_ID = "13412"
        const val GSAK_SAKSNUMMER = 132L
        val NÅ: LocalDate = LocalDate.now()
        val NESTE_ÅR: LocalDate = LocalDate.now().plusYears(1)
    }
}
