package no.nav.melosys.service.sak

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TrygdeavgiftOppsummeringServiceTest {

    private val SAKSNUMMER = "MEL-123"
    private val BEHANDLING_ID = 123L

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService

    private lateinit var fagsak: Fagsak
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    fun setup() {
        trygdeavgiftOppsummeringService = TrygdeavgiftOppsummeringService(fagsakService, behandlingsresultatService)
        fagsak = Fagsak().apply { behandlinger.add(Behandling().apply { id = BEHANDLING_ID }) }
        behandlingsresultat = Behandlingsresultat()
        every { fagsakService.hentFagsak(SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harIkkeMedlemAvFolketrygden_returnererFalse() {
        behandlingsresultat.apply { medlemAvFolketrygden = null }


        trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeSattMndBeløp_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply { trygdeavgiftsbeløpMd = null }
        behandlingsresultat.apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                fastsattTrygdeavgift = FastsattTrygdeavgift().apply { trygdeavgiftsperioder.add(trygdeavgiftsperiode) }
            }
        }


        trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeAvgift_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(0.0)
            trygdesats = BigDecimal.ZERO
        }
        behandlingsresultat.apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                fastsattTrygdeavgift = FastsattTrygdeavgift().apply { trygdeavgiftsperioder.add(trygdeavgiftsperiode) }
            }
        }


        trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeBestiltFaktura_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(2345.56)
            trygdesats = BigDecimal(3.56)
        }
        behandlingsresultat.apply {
            medlemAvFolketrygden = MedlemAvFolketrygden().apply {
                fastsattTrygdeavgift = FastsattTrygdeavgift().apply { trygdeavgiftsperioder.add(trygdeavgiftsperiode) }
            }
        }


        trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperioderBådeMedOgUtenAvgiftOgFakturaErBestilt_returnererTrue() {
        val trygdeavgiftsperiode1 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(0.0)
            trygdesats = BigDecimal.ZERO
        }
        val trygdeavgiftsperiode2 = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(2345.56)
            trygdesats = BigDecimal(3.56)
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


        trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(SAKSNUMMER).shouldBeTrue()
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

        trygdeavgiftOppsummeringService.hentEksisterendeTrygdeavgiftsperioderForFagsak(saksnummer, year).shouldBe(setOf(trygdeavgiftsperiode2))
    }
}
