package no.nav.melosys.service.sak

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class TrygdeavgiftServiceTest {

    private val BEHANDLING_ID = 123L

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var trygdeavgiftService: TrygdeavgiftService

    private lateinit var fagsak: Fagsak
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    fun setup() {
        trygdeavgiftService = TrygdeavgiftService(fagsakService, behandlingsresultatService)
        fagsak = FagsakTestFactory.builder().apply { leggTilBehandling(Behandling().apply { id = BEHANDLING_ID }) }.build()
        behandlingsresultat = Behandlingsresultat()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harIkkeMedlemAvFolketrygden_returnererFalse() {
        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeSattMndBeløp_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply { trygdeavgiftsbeløpMd = null }
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode)
                }
            )
        }

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeAvgift_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(0.0)
            trygdesats = BigDecimal.ZERO
        }
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode)
                }
            )
        }

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperiodeMenDenHarIkkeBestiltFaktura_returnererFalse() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(2345.56)
            trygdesats = BigDecimal(3.56)
        }
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode)
                }
            )
        }

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
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
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode1)
                    trygdeavgiftsperioder.add(trygdeavgiftsperiode2)
                    fakturaserieReferanse = "FakturaserieReferanse"
                }
            )
        }


        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeTrue()
    }
}
