package no.nav.melosys.service.dokument.brev.mapper


import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
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


        // kun reelle verdier for (tidligereFakturertBeloep, trygdeavgiftFraAvgiftssystemet, beregnetAvgiftBelop, tilFaktureringBeloep)
        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harTrygdeavgiftFraAvgiftssystemet = true,
            trygdeavgiftFraAvgiftssystemet = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
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


        // kun reelle verdier for (tidligereFakturertBeloep, trygdeavgiftFraAvgiftssystemet, nyttTotalbeloep, tilFaktureringBeloep)
        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harTrygdeavgiftFraAvgiftssystemet = true,
            trygdeavgiftFraAvgiftssystemet = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(1652),
            endeligAvgiftValg = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
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
        val (brevbestilling, behandlingsresultat) = lagFellesTestdata()

        // Opprett en tidligere årsavregningsbehandling
        val tidligereÅrsavregningsBehandling = lagBehandling().apply {
            id = 999L
            type = Behandlingstyper.ÅRSAVREGNING
        }
        val tidligereBehandlingsresultat = mockk<Behandlingsresultat>().apply {
            every { behandling } returns tidligereÅrsavregningsBehandling
        }

        // Sett opp nåværende årsavregning til å referere til den tidligere
        every { behandlingsresultat.hentÅrsavregning().tidligereBehandlingsresultat } returns tidligereBehandlingsresultat

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
            tidligereGrunnlag = grunnlagMedlemskap,
            tidligereAvgift = tidligereAvgift,
            nyttGrunnlag = grunnlagMedlemskap,
            endeligAvgift = endeligAvgift,
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harTrygdeavgiftFraAvgiftssystemet = true,
            trygdeavgiftFraAvgiftssystemet = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(1652),
            endeligAvgiftValg = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
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
        val endeligAvgiftTrygdeavgiftsperiode = lagEndeligTrygdeavgiftsperiode().apply {
            grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt = betalesTilSkatt
            grunnlagMedlemskapsperiode!!.medlemskapstype = medlemskapstype
        }

        val årsavregningModel = ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tidligereGrunnlag = lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode),
            tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode()),
            nyttGrunnlag = lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode),
            endeligAvgift = listOf(endeligAvgiftTrygdeavgiftsperiode),
            tidligereFakturertBeloep = BigDecimal(2652),
            beregnetAvgiftBelop = BigDecimal(11699.91),
            tilFaktureringBeloep = BigDecimal(7047.91),
            harTrygdeavgiftFraAvgiftssystemet = true,
            trygdeavgiftFraAvgiftssystemet = BigDecimal(2000),
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        every { årsavregningService.finnÅrsavregningForBehandling(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)
        result.shouldNotBeNull()
        behandlingsresultat.hentÅrsavregning().aar shouldBe result.årsavregningsår

        result.endeligTrygdeavgift[0].arbeidsgiveravgiftBetalt shouldBe forventetVerdi
    }

    private fun lagGrunnlagMedlemskap(endeligAvgiftTrygdeavgiftsperiode: Trygdeavgiftsperiode) =
        Trygdeavgiftsgrunnlag(
            listOf(MedlemskapsperiodeForAvgift(endeligAvgiftTrygdeavgiftsperiode.grunnlagMedlemskapsperiode!!)),
            emptyList(),
            emptyList()
        )


    private fun lagFellesTestdata(): Pair<ÅrsavregningVedtakBrevBestilling, Behandlingsresultat> {
        val brevbestilling = ÅrsavregningVedtakBrevBestilling.Builder()
            .medPersonDokument(PersonDokument().apply { sammensattNavn = "Hei Test" })
            .medPersonMottaker(PersonDokument().apply { sammensattNavn = "Hei Test" })
            .medBehandling(lagBehandling())
            .build()
        val behandlingsresultat = lagBehandlingsresultat()
        return Pair(brevbestilling, behandlingsresultat)
    }

    private fun lagBehandlingsresultat(): Behandlingsresultat {
        val fagsak = mockk<Fagsak>(relaxed = true)
        every { fagsak.erSakstypeEøs() } returns true
        every { fagsak.erSakstypeTrygdeavtale() } returns false

        val behandling = mockk<Behandling>(relaxed = true)
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>(relaxed = true)
        every { årsavregning.aar } returns 2023

        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.behandling = behandling
        behandlingsresultat.årsavregning = årsavregning

        return behandlingsresultat
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
            nyttGrunnlag = grunnlagMedlemskap,
            beregnetAvgiftBelop = beregnetAvgiftBelop,
            tidligereFakturertBeloep = tidligereFakturertBeloep,
            tidligereGrunnlag = grunnlagMedlemskap,
            harTrygdeavgiftFraAvgiftssystemet = false,
            trygdeavgiftFraAvgiftssystemet = null,
            manueltAvgiftBeloep = BigDecimal(0),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )
    }

    private fun lagEndeligTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal(500)),
            trygdesats = BigDecimal(1000),

            grunnlagInntekstperiode = Inntektsperiode().apply {
                avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
            },

            grunnlagMedlemskapsperiode = Medlemskapsperiode().apply {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
                trygdedekning = Trygdedekninger.FULL_DEKNING
            },

            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            })
    }

    private fun lagTidligereTrygdeavgiftsperiode() =
        Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal(450)),
            trygdesats = BigDecimal(900),

            grunnlagInntekstperiode = Inntektsperiode().apply {
                avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = false
            },

            grunnlagMedlemskapsperiode = Medlemskapsperiode().apply {
                trygdedekning = Trygdedekninger.FULL_DEKNING
            },

            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            })
}
