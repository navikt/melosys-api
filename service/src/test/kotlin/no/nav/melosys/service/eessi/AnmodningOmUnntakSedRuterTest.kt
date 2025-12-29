package no.nav.melosys.service.eessi

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.lovvalgsperiode
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
        val melding = MelosysEessiMelding(aktoerId = AKTØR_ID)
        val prosessinstans = lagProsessinstans(melding)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify {
            prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(
                melding,
                melding.aktoerId
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererPeriodeEndret nyBehandling`() {
        val fagsak = lagFagsak()
        val melding = lagMelosysEessiMelding(NÅ, NESTE_ÅR)
        val prosessinstans = lagProsessinstans(melding, fagsak.behandlinger[0])
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns lagBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR.plusDays(1))
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify { prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(melding, GSAK_SAKSNUMMER) }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererPeriodeIkkeEndret ikkeNyBehandling`() {
        val fagsak = lagFagsak()
        val melding = lagMelosysEessiMelding(NÅ, NESTE_ÅR)
        val prosessinstans = lagProsessinstans(melding, fagsak.behandlinger[0])
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns lagBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR)
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify(exactly = 0) { prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(any(), any()) }
        verify { prosessinstansService.opprettProsessinstansSedJournalføring(fagsak.behandlinger[0], melding) }
    }

    @Test
    fun `finnSakOgBestemRuting sakEksistererIkke nySak`() {
        val melding = MelosysEessiMelding()
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melding)
            medData(ProsessDataKey.AKTØR_ID, AKTØR_ID)
        }
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.empty()

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify { prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(melding, AKTØR_ID) }
    }

    private fun lagProsessinstans(melding: MelosysEessiMelding, behandling: Behandling? = null) =
        Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melding)
            behandling?.let { this.behandling = it }
        }

    private fun lagBehandlingsresultatMedLovvalgsperiode(fom: LocalDate, tom: LocalDate) =
        Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.SE
                this.fom = fom
                this.tom = tom
            }
        }

    private fun lagFagsak(): Fagsak =
        Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.OPPRETTET
            fagsak { }
        }.fagsak

    private fun lagMelosysEessiMelding(fom: LocalDate, tom: LocalDate) = MelosysEessiMelding(
        aktoerId = AKTØR_ID,
        artikkel = "12_1",
        dokumentId = "123321",
        journalpostId = "j123",
        lovvalgsland = "SE",
        periode = Periode(fom = fom, tom = tom),
        rinaSaksnummer = "r123",
        sedId = "s123",
        statsborgerskap = Collections.singletonList(Statsborgerskap("SE")),
        sedType = "A001",
        bucType = "LA_BUC_01"
    )

    companion object {
        const val AKTØR_ID = "13412"
        const val GSAK_SAKSNUMMER = 132L
        val NÅ: LocalDate = LocalDate.now()
        val NESTE_ÅR: LocalDate = LocalDate.now().plusYears(1)
    }
}
