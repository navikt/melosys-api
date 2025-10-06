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
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
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
    private val BEHANDLINGSRESULTAT_ID = 123L

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
        fagsak = Fagsak.forTest { behandling { id = BEHANDLING_ID } }
        behandlingsresultat = Behandlingsresultat().apply { id = BEHANDLINGSRESULTAT_ID }
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) }.returns(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, uten avgiftsperioder, returnerer false`() {
        trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(FagsakTestFactory.SAKSNUMMER).shouldBeFalse()
    }

    @Test
    fun `harFagsakBehandlingerMedTrygdeavgift, med avgiftsperioder men uten mndBeløp, returnerer false`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            trygdeavgiftsbeløpMd = Penger(0.0),
            trygdesats = BigDecimal(3.5),
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
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
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            trygdeavgiftsbeløpMd = Penger(0.0),
            trygdesats = BigDecimal.ZERO,
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
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
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            trygdeavgiftsbeløpMd = Penger(30000.0),
            trygdesats = BigDecimal(3.56),
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
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
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            trygdeavgiftsbeløpMd = Penger(2345.56),
            trygdesats = BigDecimal(3.56),
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
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
        val periodeUtenAvgift = lagTrygdeavgift().copyEntity(
            trygdeavgiftsbeløpMd = Penger(0.0),
            trygdesats = BigDecimal.ZERO
        )
        val periodeMedAvgift = lagTrygdeavgift().copyEntity(
            trygdeavgiftsbeløpMd = Penger(2345.56),
            trygdesats = BigDecimal(3.56)
        )
        behandlingsresultat.apply {
            medlemskapsperioder.add(
                Medlemskapsperiode().apply {
                    medlemskapstype = Medlemskapstyper.PLIKTIG
                    trygdeavgiftsperioder.add(periodeUtenAvgift)
                    trygdeavgiftsperioder.add(periodeMedAvgift)
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
        val ingenAvgift = Trygdeavgiftsperiode(
            trygdeavgiftsbeløpMd = Penger(0.0),
            trygdesats = BigDecimal.ZERO,
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
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

    @Test
    fun `harFakturerbarTrygdeavgift - EØS pensjonist, trygdeavgift + betaler til NAV, true`() {
        behandlingsresultat.apply {
            behandling = Behandling.forTest {
                id = BEHANDLING_ID
                tema = Behandlingstema.PENSJONIST
                fagsak = Fagsak.forTest {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                }
            }

            helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
                behandlingsresultat = this,
                fomDato = LocalDate.of(2023, 1, 1),
                tomDato = LocalDate.of(2023, 5, 1),
                bostedLandkode = Land_iso2.NO
            )

            eøsPensjonistTrygdeavgiftsperioder.add(lagTrygdeavgift())
        }

        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat).shouldBeTrue()
    }

    private fun lagTrygdeavgift(): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2023, 5, 1),
        trygdeavgiftsbeløpMd = Penger(5000.0),
        trygdesats = BigDecimal(3.5),
        grunnlagInntekstperiode = lagInntektsperiode(),
        grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge()
    )

    private fun lagInntektsperiode(): Inntektsperiode = Inntektsperiode().apply {
        fomDato = LocalDate.of(2023, 1, 1)
        tomDato = LocalDate.of(2023, 5, 1)
        avgiftspliktigMndInntekt = Penger(5000.0)
    }

    private fun lagSkatteforholdTilNorge(): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.of(2022, 1, 1)
        tomDato = LocalDate.of(2023, 5, 31)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }
}
