package no.nav.melosys.service.avgift.satsendring

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class SatsendringProsessGeneratorTest {
    @MockK
    private lateinit var satsendringFinner: SatsendringFinner

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var satsendringProsessGenerator: SatsendringProsessGenerator

    @BeforeEach
    fun setUp() {
        satsendringProsessGenerator = SatsendringProsessGenerator(
            satsendringFinner,
            behandlingService,
            prosessinstansService
        )
    }

    @Test
    fun `opprettSatsendringProsesserForAar skal opprette prosesser for behandlinger med satsendring`() {
        // Given
        val år = 2023
        val behandlingID1 = 1L
        val behandlingID2 = 2L
        val saksnummer1 = "SAK1"
        val saksnummer2 = "SAK2"

        val fagsak1 = FagsakTestFactory.builder().saksnummer(saksnummer1).build()
        val behandling1 = Behandling().apply {
            id = behandlingID1
            fagsak = fagsak1
        }

        val fagsak2 = FagsakTestFactory.builder().saksnummer(saksnummer2).build()
        val behandling2 = Behandling().apply {
            id = behandlingID2
            fagsak = fagsak2
        }

        val behandlingerMedSatsendring = listOf(
            SatsendringFinner.BehandlingInfo(
                behandlingID = behandlingID1,
                saksnummer = saksnummer1,
                behandlingstype = Behandlingstyper.FØRSTEGANG,
                påvirketAvSatsendring = true,
                harAnnenAktivBehandling = false
            )
        )

        val behandlingerMedSatsendringOgNyVurdering = listOf(
            SatsendringFinner.BehandlingInfo(
                behandlingID = behandlingID2,
                saksnummer = saksnummer2,
                behandlingstype = Behandlingstyper.NY_VURDERING,
                påvirketAvSatsendring = true,
                harAnnenAktivBehandling = true
            )
        )

        val avgiftSatsendringInfo = SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingerMedSatsendring,
            behandlingerMedSatsendringOgNyVurdering = behandlingerMedSatsendringOgNyVurdering,
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )

        // When
        every { satsendringFinner.finnBehandlingerMedSatsendring(år) } returns avgiftSatsendringInfo
        every { behandlingService.hentBehandling(behandlingID1) } returns behandling1
        every { behandlingService.hentBehandling(behandlingID2) } returns behandling2
        every { prosessinstansService.opprettSatsendringBehandlingFor(behandling1, år) } returns UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        every { prosessinstansService.opprettSatsendringBehandlingNyVurderingFor(behandling2, år) } returns UUID.fromString("123e4567-e89b-12d3-a456-426614174001")

        satsendringProsessGenerator.opprettSatsendringsprosesserForÅr(år, dryRun = false)

        // Then
        verify(exactly = 1) { prosessinstansService.opprettSatsendringBehandlingFor(behandling1, år) }
        verify(exactly = 1) { prosessinstansService.opprettSatsendringBehandlingNyVurderingFor(behandling2, år) }
    }

    @Test
    fun `opprettSatsendringProsesserForAar med dry run skal ikke opprette prosesser`() {
        // Given
        val år = 2024
        val avgiftSatsendringInfo = SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = listOf(
                SatsendringFinner.BehandlingInfo(1L, "SAK1", Behandlingstyper.FØRSTEGANG, true, false)
            ),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                SatsendringFinner.BehandlingInfo(2L, "SAK2", Behandlingstyper.NY_VURDERING, true, true)
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )

        every { satsendringFinner.finnBehandlingerMedSatsendring(år) } returns avgiftSatsendringInfo
        every { behandlingService.hentBehandling(any()) } returns Behandling()

        // When
        satsendringProsessGenerator.opprettSatsendringsprosesserForÅr(år, dryRun = true)

        // Then
        verify { prosessinstansService wasNot Called }
    }
}
