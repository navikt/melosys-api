package no.nav.melosys.service.avgift

import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.*
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.lagBehandlingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class TrygdeavgiftMottakerServiceTest {

    @MockK
    lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    @BeforeEach
    fun setUp() {
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker ikke er skattepliktig og aga er false`() {

        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )

        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)

        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis bruker er skattepliktig og aga er true`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT hvis bruker er skattepliktig og aga er false, men type er MISJONÆR`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, MISJONÆR)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT og NAV type er MISJONÆR kun i en periode`() {

        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, MISJONÆR)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, MISJONÆR)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker er ikke skattepliktig, men type er FN_SKATTEFRITAK`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, FN_SKATTEFRITAK)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker er skattepliktig, men type er FN_SKATTEFRITAK`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, FN_SKATTEFRITAK)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker er ikke_skattepliktig og aga er false, men type er FN_SKATTEFRITAK`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, FN_SKATTEFRITAK)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT og NAV hvis bruker har flere inntektsperioder, men en type er FN_SKATTEFRITAK`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, FN_SKATTEFRITAK)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV hvis bruker er ikke skattepliktig og aga er false, men type er MISJONÆR`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, MISJONÆR)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v1`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v2`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v3`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                ).toMutableSet()
            }
        )
        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v4`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                ).toMutableSet()
            }
        )

        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis skatteplikt og aga er en kombinasjon v5`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                ).toMutableSet()
            }
        )

        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT hvis det er flere innteksperioder med forskjellige mottakere`() {
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
                    ),
                    lagTrygdeavgiftsperiode(
                        lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG),
                        lagInntektsperiode(false, ARBEIDSINNTEKT_FRA_NORGE)
                    )
                ).toMutableSet()
            }
        )

        val result = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat)
        result.shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være NAV og SKATT når samlet beregnet periode har inntekt uten aga, selv om siste grunnlag har aga`() {
        val skattepliktig = lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG)
        val inntektFraUtlandet = lagInntektsperiode(false, INNTEKT_FRA_UTLANDET)
        val arbeidsinntektFraNorge = lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
        val samletPeriode = lagTrygdeavgiftsperiode(skattepliktig, arbeidsinntektFraNorge).apply {
            leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(trygdeavgiftsperiode = this, inntektsperiode = inntektFraUtlandet, skatteforhold = skattepliktig))
            leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(trygdeavgiftsperiode = this, inntektsperiode = arbeidsinntektFraNorge, skatteforhold = skattepliktig))
        }
        val behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()
        behandlingsresultat.medlemskapsperioder.add(
            Medlemskapsperiode().apply { trygdeavgiftsperioder = mutableSetOf(samletPeriode) }
        )

        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(listOf(samletPeriode)).shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat).shouldBe(TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    @Test
    fun `trygdeavgiftsmottaker skal være SKATT når alle inntekter i samlet beregnet periode har aga og bruker er skattepliktig`() {
        val skattepliktig = lagSkatteforholdsperiode(Skatteplikttype.SKATTEPLIKTIG)
        val inntekt1 = lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
        val inntekt2 = lagInntektsperiode(true, ARBEIDSINNTEKT_FRA_NORGE)
        val samletPeriode = lagTrygdeavgiftsperiode(skattepliktig, inntekt2).apply {
            leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(trygdeavgiftsperiode = this, inntektsperiode = inntekt1, skatteforhold = skattepliktig))
            leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(trygdeavgiftsperiode = this, inntektsperiode = inntekt2, skatteforhold = skattepliktig))
        }

        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(listOf(samletPeriode)).shouldBe(TRYGDEAVGIFT_BETALES_TIL_SKATT)
    }

    fun lagTrygdeavgiftsperiode(
        skatteforholdTilNorge: SkatteforholdTilNorge,
        inntektsperiode: Inntektsperiode
    ) = Trygdeavgiftsperiode(
        grunnlagInntekstperiode = inntektsperiode,
        grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
        periodeTil = LocalDate.now(),
        periodeFra = LocalDate.now(),
        trygdesats = BigDecimal(1),
        trygdeavgiftsbeløpMd = Penger(BigDecimal(1), NOK.kode)
    )

    fun lagInntektsperiode(
        arbeidsgiversavgiftBetalesTilSkatt: Boolean,
        inntektskildetype: Inntektskildetype
    ): Inntektsperiode {
        val inntektsperiode = Inntektsperiode().apply {
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt
            type = inntektskildetype
        }
        return inntektsperiode
    }

    fun lagSkatteforholdsperiode(
        skatteplikttype: Skatteplikttype
    ): SkatteforholdTilNorge {
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            setSkatteplikttype(skatteplikttype)
        }
        return skatteforholdTilNorge
    }
}
