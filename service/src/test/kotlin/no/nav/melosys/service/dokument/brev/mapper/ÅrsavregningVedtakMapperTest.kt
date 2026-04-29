package no.nav.melosys.service.dokument.brev.mapper


import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.domain.tidligereBehandlingsresultat
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.SvarAlternativ
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.aarsavregning.MedlemskapsperiodeForAvgift
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class ÅrsavregningVedtakMapperTest {
    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    @MockK
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService
    private lateinit var mapper: ÅrsavregningVedtakMapper

    @BeforeEach
    fun setUp() {
        mapper = ÅrsavregningVedtakMapper(årsavregningService, trygdeavgiftsberegningService)
        every { trygdeavgiftsberegningService.finnFakturamottakerNavn(any()) } returns "Fakturamannen"
    }

    @Test
    fun `mapÅrsavregning kaster FunksjonellException når årsavregning ikke funnet`() {
        val brevbestilling = ÅrsavregningVedtakBrevBestilling.Builder()
            .medBehandling(lagBehandling())
            .build()
        val behandlingsresultat = lagBehandlingsresultat()

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns null

        val exception = assertThrows<FunksjonellException> {
            mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)
        }

        exception.message shouldBe "Finner ingen årsavregning for behandling ${brevbestilling.behandlingId}"
    }

    @Test
    fun `mapÅrsavregning skal mappe til ÅrsavregningVedtaksbrev når det finnes en årsavregning, bruker skylder penger`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val nyttTotalbeløp = BigDecimal(2000)
        val tidligereFakturertBeløp = BigDecimal(1000)

        val årsavregningModel = lagÅrsavregningModel(nyttTotalbeløp, tidligereFakturertBeløp)
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()
        behandlingsresultat.hentÅrsavregning().aar shouldBe result.årsavregningsår

        result.endeligTrygdeavgift[0] shouldBe Avgiftsperiode(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
            avgiftssats = BigDecimal(1000),
            avgiftPerMd = BigDecimal(500),
            avgiftspliktigInntektPerMd = BigDecimal(2800),
            inntektskilde = Inntektskildetype.INNTEKT_FRA_UTLANDET.beskrivelse,
            trygdedekning = Trygdedekninger.FULL_DEKNING.beskrivelse,
            arbeidsgiveravgiftBetalt = SvarAlternativ.JA,
            skatteplikt = true
        )
        result.forskuddsvisFakturertTrygdeavgift[0] shouldBe Avgiftsperiode(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
            avgiftssats = BigDecimal(900),
            avgiftPerMd = BigDecimal(450),
            avgiftspliktigInntektPerMd = BigDecimal(2800),
            inntektskilde = Inntektskildetype.INNTEKT_FRA_UTLANDET.beskrivelse,
            trygdedekning = Trygdedekninger.FULL_DEKNING.beskrivelse,
            arbeidsgiveravgiftBetalt = SvarAlternativ.NEI,
            skatteplikt = false
        )

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.beregnetAvgiftBelop
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe årsavregningModel.tidligereFakturertBeloep
        result.differansebeløp shouldBe BigDecimal(1000)
        result.minimumsbeløpForFakturering shouldBe BigDecimal(100)
        result.pliktigMedlemskap shouldBe false
        result.eøsEllerTrygdeavtale shouldBe true
    }

    @Test
    fun `mapÅrsavregning skal mappe til ÅrsavregningVedtaksbrev når det finnes en årsavregning, bruker får tilbake penger`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val nyttTotalbeløp = BigDecimal(1000)
        val tidligereFakturertBeløp = BigDecimal(2000)

        val årsavregningModel = lagÅrsavregningModel(nyttTotalbeløp, tidligereFakturertBeløp)
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.beregnetAvgiftBelop
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe årsavregningModel.tidligereFakturertBeloep
        result.differansebeløp shouldBe BigDecimal(-1000)
    }

    @Test
    fun `mapÅrsavregning skal mappe til ÅrsavregningVedtaksbrev når vi har delt grunnlag`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode())
        val tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())


        // kun reelle verdier for (tidligereFakturertBeloep, innbetaltTrygdeavgift, beregnetAvgiftBelop, tilFaktureringBeloep)
        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harInnbetaltTrygdeavgift = true,
            innbetaltTrygdeavgift = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.beregnetAvgiftBelop
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe BigDecimal(4652)
        result.differansebeløp shouldBe BigDecimal(7047.91)
        result.erNyÅrsavregning shouldBe false
    }

    @Test
    fun `mapÅrsavregning skal mappe manuell årsavregning til ÅrsavregningVedtaksbrev når vi har delt grunnlag`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode())
        val tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())


        // kun reelle verdier for (tidligereFakturertBeloep, innbetaltTrygdeavgift, nyttTotalbeloep, tilFaktureringBeloep)
        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harInnbetaltTrygdeavgift = true,
            innbetaltTrygdeavgift = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(1652),
            endeligAvgiftValg = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()

        result.endeligTrygdeavgift shouldBe emptyList()
        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.manueltAvgiftBeloep
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe BigDecimal(4652)
        result.differansebeløp shouldBe BigDecimal(7047.91)
        result.erNyÅrsavregning shouldBe false
    }


    @Test
    fun `mapÅrsavregningVedtak setter korrekt arbeidsgiveravgiftBetalt verdi`() {
        testArbeidsgiveravgiftBetalt(Medlemskapstyper.PLIKTIG, false, SvarAlternativ.IKKE_RELEVANT)
        testArbeidsgiveravgiftBetalt(Medlemskapstyper.FRIVILLIG, false, SvarAlternativ.NEI)
        testArbeidsgiveravgiftBetalt(Medlemskapstyper.FRIVILLIG, true, SvarAlternativ.JA)
    }

    @Test
    fun `mapÅrsavregning skal detektere automatisk re-årsavregning korrekt når tidligereBehandlingsresultat finnes`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata {
            årsavregning {
                aar = 2023
                tidligereBehandlingsresultat {
                    behandling {
                        id = 999L
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
        }

        val årsavregningModel = lagÅrsavregningModel(BigDecimal(1000), BigDecimal(500))
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()
        result.erNyÅrsavregning shouldBe true
    }

    @Test
    fun `mapÅrsavregning skal detektere automatisk førstegangshåndtering korrekt når ingen tidligere årsavregning finnes`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val årsavregningModel = lagÅrsavregningModel(BigDecimal(1000), BigDecimal(500))
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()
        result.erNyÅrsavregning shouldBe false
    }

    @Test
    fun `mapÅrsavregning skal detektere manuell re-årsavregning korrekt når tidligereÅrsavregningmanueltAvgiftBeloep finnes`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        val endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode())
        val tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())

        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harInnbetaltTrygdeavgift = true,
            innbetaltTrygdeavgift = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(1652),
            endeligAvgiftValg = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = BigDecimal(1500), // Dette gjør at erNyÅrsavregning blir true
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()
        result.erNyÅrsavregning shouldBe true
        result.endeligTrygdeavgift shouldBe emptyList()
        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.manueltAvgiftBeloep
    }


    fun svaralternativpermutations(): Stream<Arguments> = Stream.of(
        Arguments.of(Medlemskapstyper.PLIKTIG, false, SvarAlternativ.IKKE_RELEVANT),
        Arguments.of(Medlemskapstyper.FRIVILLIG, false, SvarAlternativ.NEI),
        Arguments.of(Medlemskapstyper.FRIVILLIG, true, SvarAlternativ.JA)
    )

    @ParameterizedTest(name = "{0} - {1} - {2}")
    @MethodSource("svaralternativpermutations")
    fun testArbeidsgiveravgiftBetalt(
        medlemskapstype: Medlemskapstyper,
        betalesTilSkatt: Boolean,
        forventetVerdi: SvarAlternativ?
    ) {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val grunnlagMedlemskapsperiode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 12, 31)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            trygdedekning = Trygdedekninger.FULL_DEKNING
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            this.medlemskapstype = medlemskapstype
        }

        val endeligAvgiftTrygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 12, 31)
            trygdeavgiftsbeløpMd = BigDecimal(500)
            trygdesats = BigDecimal(1000)
            this.medlemskapsperiode = grunnlagMedlemskapsperiode
            grunnlagInntekstperiode {
                avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                arbeidsgiversavgiftBetalesTilSkatt = betalesTilSkatt
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }

        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereTrygdeavgiftsGrunnlag = lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode()),
            nyttTrygdeavgiftsGrunnlag = lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode),
            endeligAvgift = listOf(endeligAvgiftTrygdeavgiftsperiode),
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harInnbetaltTrygdeavgift = true,
            innbetaltTrygdeavgift = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)
        result.shouldNotBeNull()
        behandlingsresultat.hentÅrsavregning().aar shouldBe result.årsavregningsår

        result.endeligTrygdeavgift[0].arbeidsgiveravgiftBetalt shouldBe forventetVerdi
    }

    @Test
    fun `mapÅrsavregning mapper MINSTEBELØP beregningsregel og minstebeløp i endeligTrygdeavgift`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModelMedPerioder(
            endeligAvgift = listOf(lagMinstebelopTrygdeavgiftsperiode()),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())
        )
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.endeligTrygdeavgift shouldHaveSize 1
        result.endeligTrygdeavgift[0].beregningsregel shouldBe "MINSTEBELØP"
        result.minstebelopVerdi shouldBe BigDecimal(7000)
        result.minstebelopAar shouldBe 2024
    }

    @Test
    fun `mapÅrsavregning mapper TJUEFEM_PROSENT_REGEL beregningsregel i endeligTrygdeavgift`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModelMedPerioder(
            endeligAvgift = listOf(lagTjuefemProsentTrygdeavgiftsperiode()),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())
        )
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.endeligTrygdeavgift shouldHaveSize 1
        result.endeligTrygdeavgift[0].beregningsregel shouldBe "TJUEFEM_PROSENT_REGEL"
        result.minstebelopVerdi shouldBe BigDecimal(7000)
        result.minstebelopAar shouldBe 2024
    }

    @Test
    fun `mapÅrsavregning mapper beregningsregel og minstebeløp i forskuddsvisFakturertTrygdeavgift`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModelMedPerioder(
            endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode()),
            tidligereAvgift = listOf(lagMinstebelopTrygdeavgiftsperiode())
        )
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.forskuddsvisFakturertTrygdeavgift shouldHaveSize 1
        result.forskuddsvisFakturertTrygdeavgift[0].beregningsregel shouldBe "MINSTEBELØP"
        result.minstebelopVerdi shouldBe BigDecimal(7000)
    }

    @Test
    fun `mapÅrsavregning mapper ORDINÆR beregningsregel til null på DTO`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModel(BigDecimal(2000), BigDecimal(1000))
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.endeligTrygdeavgift[0].beregningsregel shouldBe null
        result.forskuddsvisFakturertTrygdeavgift[0].beregningsregel shouldBe null
    }

    @Test
    fun `mapÅrsavregning setter harMisjonaerInntekt true når en periode har MISJONÆR-inntekt`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModelMedPerioder(
            endeligAvgift = listOf(lagMisjonaerTrygdeavgiftsperiode()),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())
        )
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.harMisjonaerInntekt shouldBe true
    }

    @Test
    fun `mapÅrsavregning setter minstebelopVerdi og minstebelopAar på rot når en periode har beregningsregel`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModelMedPerioder(
            endeligAvgift = listOf(lagMinstebelopTrygdeavgiftsperiode()),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())
        )
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.minstebelopVerdi shouldBe BigDecimal(7000)
        result.minstebelopAar shouldBe 2024
    }

    @Test
    fun `mapÅrsavregning setter minstebelopVerdi og minstebelopAar til null når ingen perioder har beregningsregel`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModel(BigDecimal(2000), BigDecimal(1000))
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.minstebelopVerdi shouldBe null
        result.minstebelopAar shouldBe null
    }

    @Test
    fun `mapÅrsavregning setter harMisjonaerInntekt false når ingen perioder har MISJONÆR-inntekt`() {
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()
        val årsavregningModel = lagÅrsavregningModel(BigDecimal(2000), BigDecimal(1000))
        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.harMisjonaerInntekt shouldBe false
    }

    private fun lagÅrsavregningModelMedPerioder(
        endeligAvgift: List<Trygdeavgiftsperiode>,
        tidligereAvgift: List<Trygdeavgiftsperiode>
    ): ÅrsavregningModel {
        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())
        return ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tilFaktureringBeloep = BigDecimal(1000),
            endeligAvgift = endeligAvgift,
            tidligereAvgift = tidligereAvgift,
            nyttTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            beregnetAvgiftBelop = BigDecimal(2000),
            tidligereFakturertBeloep = BigDecimal(1000),
            tidligereTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            harInnbetaltTrygdeavgift = false,
            innbetaltTrygdeavgift = null,
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )
    }

    private fun lagMinstebelopTrygdeavgiftsperiode(): Trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
        periodeFra = LocalDate.of(2024, 1, 1)
        periodeTil = LocalDate.of(2024, 12, 31)
        trygdesats = null
        trygdeavgiftsbeløpMd = BigDecimal.ZERO
        beregningsregel = Avgiftsberegningsregel.MINSTEBELØP
        minstebelopVerdi = BigDecimal(7000)
        minstebelopAar = 2024
        medlemskapsperiode = medlemskapsperiodeForTest {
            trygdedekning = Trygdedekninger.FULL_DEKNING
        }
        grunnlagInntekstperiode {
            avgiftspliktigMndInntekt = Penger(BigDecimal(500), NOK.kode)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            arbeidsgiversavgiftBetalesTilSkatt = false
        }
        grunnlagSkatteforholdTilNorge {
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    }

    private fun lagTjuefemProsentTrygdeavgiftsperiode(): Trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
        periodeFra = LocalDate.of(2024, 1, 1)
        periodeTil = LocalDate.of(2024, 12, 31)
        trygdesats = null
        trygdeavgiftsbeløpMd = BigDecimal(250)
        beregningsregel = Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
        minstebelopVerdi = BigDecimal(7000)
        minstebelopAar = 2024
        medlemskapsperiode = medlemskapsperiodeForTest {
            trygdedekning = Trygdedekninger.FULL_DEKNING
        }
        grunnlagInntekstperiode {
            avgiftspliktigMndInntekt = Penger(BigDecimal(8000), NOK.kode)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            arbeidsgiversavgiftBetalesTilSkatt = false
        }
        grunnlagSkatteforholdTilNorge {
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    }

    private fun lagMisjonaerTrygdeavgiftsperiode(): Trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
        periodeFra = LocalDate.of(2024, 1, 1)
        periodeTil = LocalDate.of(2024, 12, 31)
        trygdesats = BigDecimal(900)
        trygdeavgiftsbeløpMd = BigDecimal(450)
        medlemskapsperiode = medlemskapsperiodeForTest {
            trygdedekning = Trygdedekninger.FULL_DEKNING
        }
        grunnlagInntekstperiode {
            avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
            type = Inntektskildetype.MISJONÆR
            arbeidsgiversavgiftBetalesTilSkatt = false
        }
        grunnlagSkatteforholdTilNorge {
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    }

    private fun lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode: Trygdeavgiftsperiode) =
        Trygdeavgiftsgrunnlag(
            listOf(MedlemskapsperiodeForAvgift(endeligAvgiftTrygdeavgiftsperiode.grunnlagMedlemskapsperiode!!)),
            emptyList(),
            emptyList()
        )


    private fun lagFellesTestdata(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ): Pair<ÅrsavregningVedtakBrevBestilling, Behandlingsresultat> {
        val personDokument = personDokumentForTest { sammensattNavn = "Hei Test" }
        val brevbestilling = ÅrsavregningVedtakBrevBestilling.Builder()
            .medPersonDokument(personDokument)
            .medPersonMottaker(personDokument)
            .medBehandling(lagBehandling())
            .build()
        val behandlingsresultat = lagBehandlingsresultat(init)
        return Pair(brevbestilling, behandlingsresultat)
    }

    private fun lagBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        behandling {
            fagsak { type = Sakstyper.EU_EOS }
        }
        årsavregning { aar = 2023 }
        init()
    }

    private fun lagÅrsavregningModel(beregnetAvgiftBelop: BigDecimal, tidligereFakturertBeloep: BigDecimal): ÅrsavregningModel {
        val endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode())
        val tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())

        return ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tilFaktureringBeloep = beregnetAvgiftBelop.subtract(tidligereFakturertBeloep),
            endeligAvgift = endeligAvgift,
            tidligereAvgift = tidligereAvgift,
            nyttTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            beregnetAvgiftBelop = beregnetAvgiftBelop,
            tidligereFakturertBeloep = tidligereFakturertBeloep,
            tidligereTrygdeavgiftsGrunnlag = grunnlagMedlemskap,
            harInnbetaltTrygdeavgift = false,
            innbetaltTrygdeavgift = null,
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereInnbetaltTrygdeavgift = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )
    }

    private fun lagEndeligTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        val medlemskapsperiode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 12, 31)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            trygdedekning = Trygdedekninger.FULL_DEKNING
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        return Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 12, 31)
            trygdeavgiftsbeløpMd = BigDecimal(500)
            trygdesats = BigDecimal(1000)
            this.medlemskapsperiode = medlemskapsperiode
            grunnlagInntekstperiode {
                avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                arbeidsgiversavgiftBetalesTilSkatt = true
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }
    }

    private fun lagTidligereTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        val medlemskapsperiode = medlemskapsperiodeForTest {
            trygdedekning = Trygdedekninger.FULL_DEKNING
        }

        return Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2023, 1, 1)
            periodeTil = LocalDate.of(2023, 12, 31)
            trygdeavgiftsbeløpMd = BigDecimal(450)
            trygdesats = BigDecimal(900)
            this.medlemskapsperiode = medlemskapsperiode
            grunnlagInntekstperiode {
                avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                arbeidsgiversavgiftBetalesTilSkatt = false
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        }
    }
}
