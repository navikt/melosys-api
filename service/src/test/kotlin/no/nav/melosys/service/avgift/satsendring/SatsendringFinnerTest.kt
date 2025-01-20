package no.nav.melosys.service.avgift.satsendring

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class SatsendringFinnerTest {
    @MockK
    lateinit var behandlingService: BehandlingService
    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService
    @MockK
    lateinit var trygdeavgiftService: TrygdeavgiftService
    @MockK
    lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var satsendringFinner: SatsendringFinner

    @BeforeEach
    fun setUp() {
        satsendringFinner = SatsendringFinner(
            behandlingService,
            behandlingsresultatService,
            trygdeavgiftService,
            trygdeavgiftsberegningService
        )
    }

    @Test
    fun `lag AvgiftSatsendringInfo for ny vurdering`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = Behandling().apply {
            id = 1L
            type = Behandlingstyper.NY_VURDERING
            this.fagsak = fagsak
        }
        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        every { behandlingService.hentBehandling(behandlingsresultat.id) } returns behandling
        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(behandlingsresultat, any(), any()) } returns listOf(lagTrygdeavgiftsperiode(nySats))


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    harSatsendring = true
                )
            ),
            behandlingerUtenSatsendring = emptyList()
        )
    }

    private fun lagTrygdeavgiftsperiode(sats: Double): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        id = 1L,
        periodeFra = LocalDate.of(2023, 1, 1),
        periodeTil = LocalDate.of(2023, 12, 31),
        trygdeavgiftsbeløpMd = Penger(sats * 1000),
        trygdesats = BigDecimal.valueOf(sats)
    )
}
