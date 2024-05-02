package no.nav.melosys.tjenester.gui.fagsaker

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.AarsavregningService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class AarsavregningServiceTest {
    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var fagsak: Fagsak
    private lateinit var behandlingsresultat: Behandlingsresultat

    private lateinit var aarsavregningService: AarsavregningService

    @BeforeEach
    fun setup() {
        // Initialize your service here with mocked dependencies
        aarsavregningService = AarsavregningService(fagsakService, behandlingsresultatService)
    }

    @Test
    fun `test hentEksisterendeTrygdeavgiftsperioderForFagsak filters out non-matching years`() {
        val saksnummer = "12345"
        val year = 2023

        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(1000.0)
            trygdesats = BigDecimal.ZERO
            periodeFra = LocalDate.of(2021, 1, 11)
            periodeTil = LocalDate.of(2021, 10, 11)
        }
        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(2345.56)
            trygdesats = BigDecimal(3.56)
            periodeFra = LocalDate.of(2023, 1, 11)
            periodeTil = LocalDate.of(2023, 10, 11)
        }
        behandlingsresultat.apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode1)
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode2)
                }
                fakturaserieReferanse = "FakturaserieReferanse"
            }
        }

        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat

        aarsavregningService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, year).shouldBe(setOf(trygdeavgiftsperiode2))
    }
}
