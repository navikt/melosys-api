package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.SAKSBEHANDLER_NAVN
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class InformasjonTrygdeavgiftMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockTrygdeavgiftsberegningService: TrygdeavgiftsberegningService

    @MockK
    private lateinit var mockHelseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    @MockK
    private lateinit var mockTrygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var informasjonTrygdeavgiftMapper: InformasjonTrygdeavgiftMapper
    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        unleash.disableAll()
        informasjonTrygdeavgiftMapper = InformasjonTrygdeavgiftMapper(
            mockDokgenMapperDatahenter,
            mockHelseutgiftDekkesPeriodeService,
            mockTrygdeavgiftMottakerService,
            mockTrygdeavgiftsberegningService,
            unleash
        )
    }

    @Test
    fun `mapInformasjonTrygdeavgift populer alle felter`() {
        unleash.enableAll()
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = "MEL-123"
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.now().withMonth(1)
                tomDato = LocalDate.now().withMonth(4)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().withMonth(1)
                    periodeTil = LocalDate.now().withMonth(4)
                    trygdesats = BigDecimal.ZERO
                    trygdeavgiftsbeløpMd = BigDecimal(0.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().withMonth(5)
                    periodeTil = LocalDate.now().withMonth(8)
                    trygdesats = BigDecimal(0.05)
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
            }
        }


        every { mockHelseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(any()) } returns behandlingsresultat.helseutgiftDekkesPeriode
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any<List<Trygdeavgiftsperiode>>()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat


        informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(lagBrevbestilling()).shouldNotBeNull().apply {
            fomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().fomDato
            tomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().tomDato
            trygdeavgiftMottaker shouldBe Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            betalingsvalg shouldBe Betalingstype.TREKK
            bostedLand shouldBe "Danmark"
            erNordisk shouldBe true
            avgiftsperioder shouldHaveSize 2
            harAvgiftspliktigePerioderIForegåendeÅr shouldBe false
        }
    }

    @Test
    fun `mapInformasjonTrygdeavgift helseutgiftdekkes tilbake i tid TOGGLE på`() {
        unleash.enableAll()
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = "MEL-123"
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.now().minusYears(1).withMonth(1).minusYears(1)
                tomDato = LocalDate.now().withMonth(4)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().minusYears(1).withMonth(1)
                    periodeTil = LocalDate.now().withMonth(4)
                    trygdesats = BigDecimal.ZERO
                    trygdeavgiftsbeløpMd = BigDecimal(0.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().minusYears(1).withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().minusYears(1).withMonth(5)
                    periodeTil = LocalDate.now().withMonth(8)
                    trygdesats = BigDecimal(0.05)
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().minusYears(1).withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
            }
        }


        every { mockHelseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(any()) } returns behandlingsresultat.helseutgiftDekkesPeriode
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any<List<Trygdeavgiftsperiode>>()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat


        informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(lagBrevbestilling()).shouldNotBeNull().apply {
            fomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().fomDato
            tomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().tomDato
            trygdeavgiftMottaker shouldBe Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            betalingsvalg shouldBe Betalingstype.TREKK
            bostedLand shouldBe "Danmark"
            erNordisk shouldBe true
            avgiftsperioder shouldHaveSize 2
            harAvgiftspliktigePerioderIForegåendeÅr shouldBe true
        }
    }

    @Test
    fun `mapInformasjonTrygdeavgift helseutgiftdekkes tilbake i tid TOGGLE av`() {
        unleash.disableAll()
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = "MEL-123"
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.now().minusYears(1).withMonth(1).minusYears(1)
                tomDato = LocalDate.now().withMonth(4)
                bostedLandkode = Land_iso2.DK
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().minusYears(1).withMonth(1)
                    periodeTil = LocalDate.now().withMonth(4)
                    trygdesats = BigDecimal.ZERO
                    trygdeavgiftsbeløpMd = BigDecimal(0.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().minusYears(1).withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.now().minusYears(1).withMonth(5)
                    periodeTil = LocalDate.now().withMonth(8)
                    trygdesats = BigDecimal(0.05)
                    trygdeavgiftsbeløpMd = BigDecimal(500.0)
                    grunnlagInntekstperiode {
                        fomDato = LocalDate.now().minusYears(1).withMonth(1)
                        tomDato = LocalDate.now().withMonth(4)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
            }
        }


        every { mockHelseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(any()) } returns behandlingsresultat.helseutgiftDekkesPeriode
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any<List<Trygdeavgiftsperiode>>()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat


        informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(lagBrevbestilling()).shouldNotBeNull().apply {
            fomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().fomDato
            tomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().tomDato
            trygdeavgiftMottaker shouldBe Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            betalingsvalg shouldBe Betalingstype.TREKK
            bostedLand shouldBe "Danmark"
            erNordisk shouldBe true
            avgiftsperioder shouldHaveSize 2
            harAvgiftspliktigePerioderIForegåendeÅr shouldBe false
        }
    }

    @Test
    fun `mapInformasjonTrygdeavgift ingen trygdeavgiftMottaker`() {
        unleash.enableAll()
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = "MEL-123"
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.now().withMonth(1)
                tomDato = LocalDate.now().withMonth(4)
                bostedLandkode = Land_iso2.DK
            }
        }


        every { mockHelseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(any()) } returns behandlingsresultat.helseutgiftDekkesPeriode
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any<List<Trygdeavgiftsperiode>>()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat


        informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(lagBrevbestilling()).shouldNotBeNull().apply {
            fomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().fomDato
            tomDato shouldBe behandlingsresultat.hentHelseutgiftDekkesPeriode().tomDato
            trygdeavgiftMottaker shouldBe null
            betalingsvalg shouldBe Betalingstype.TREKK
            bostedLand shouldBe "Danmark"
            erNordisk shouldBe true
            avgiftsperioder shouldHaveSize 0
            harAvgiftspliktigePerioderIForegåendeÅr shouldBe false
        }
    }

    private fun lagBrevbestilling(): DokgenBrevbestilling {
        return DokgenBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .medForsendelseMottatt(Instant.now())
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .build()
    }

}
