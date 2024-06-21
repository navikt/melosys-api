package no.nav.melosys.service.sak

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

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
        val trygdeavgiftMottakerService = TrygdeavgiftMottakerService(behandlingsresultatService)
        trygdeavgiftService = TrygdeavgiftService(fagsakService, behandlingsresultatService, trygdeavgiftMottakerService)
        fagsak = FagsakTestFactory.builder().apply { leggTilBehandling(Behandling().apply { id = BEHANDLING_ID }) }.build()
        behandlingsresultat = Behandlingsresultat()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, uten avgiftsperioder, returnerer false`() {
        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, med avgiftsperioder men uten mndBeløp, returnerer false`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(0.0)
            trygdesats = BigDecimal(3.5)
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
    fun `harFagsakBehandlingerMedTrygdeavgift, fagsak har ugyldig status, returnerer false`() {
        fagsak.apply {
            status = Saksstatuser.HENLAGT
        }
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(30000.0)
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
    fun `harFagsakBehandlingerMedTrygdeavgift med sjekk av fakturaserie, trygedavgiftsperioder men ikke bestilt faktura, returnerer false`() {
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

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER, true).shouldBeFalse()
    }

    @Test
    fun harFagsakBehandlingerMedTrygdeavgift_harTrygedavgiftsperioderBådeMedOgUtenAvgift_returnererTrue() {
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
                }
            )
        }

        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeTrue()
    }

    @Test
    fun `harFakturerbarTrygdeavgift, trygdeavgift + betaler til NAV, true`() {
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(lagTrygdeavgift())
                }
            )
        }

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeTrue()
    }

    @Test
    fun `harFakturerbarTrygdeavgift, ingen trygdeavgift, false`() {
        val ingenAvgift = Trygdeavgiftsperiode().apply {
            trygdeavgiftsbeløpMd = Penger(0.0)
            trygdesats = BigDecimal.ZERO
        }
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(ingenAvgift)
                }
            )
        }

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeFalse()
    }

    private fun lagTrygdeavgift(): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode().apply {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 5, 1)
            trygdeavgiftsbeløpMd = Penger(5000.0)
            trygdesats = BigDecimal(3.5)
            grunnlagInntekstperiode = lagInntektsperiode()
            grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge()
        }
    }

    private fun lagInntektsperiode(): Inntektsperiode {
        return Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 5, 1)
            avgiftspliktigInntektMnd = Penger(5000.0)
        }
    }

    private fun lagSkatteforholdTilNorge(): SkatteforholdTilNorge {
        return SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2022, 1, 1)
            tomDato = LocalDate.of(2023, 5, 31)
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    }
}
