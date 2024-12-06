package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate


class ÅrsavregningVedtakMapperTest {
    private lateinit var årsavregningService: ÅrsavregningService
    private lateinit var mapper: ÅrsavregningVedtakMapper

    @BeforeEach
    fun setUp() {
        årsavregningService = mockk()
        mapper = ÅrsavregningVedtakMapper(årsavregningService)
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
        behandlingsresultat.årsavregning.aar shouldBe result.årsavregningsår

        result.endeligTrygdeavgift[0] shouldBe Avgiftsperiode(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
            avgiftssats = BigDecimal(1000),
            avgiftPerMd = BigDecimal(500),
            avgiftspliktigInntektPerMd = BigDecimal(2800),
            inntektskilde = Inntektskildetype.INNTEKT_FRA_UTLANDET.beskrivelse,
            trygdedekning = Trygdedekninger.FULL_DEKNING.beskrivelse,
            arbeidsgiveravgiftBetalt = true,
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
            arbeidsgiveravgiftBetalt = false,
            skatteplikt = false
        )

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.nyttTotalbeloep
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe årsavregningModel.tidligereFakturertBeloep
        result.differansebeløp shouldBe BigDecimal(1000)
        result.minimumsbeløpForFakturering shouldBe BigDecimal(100)
        result.pliktigMedlemskap shouldBe true
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

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.nyttTotalbeloep
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe årsavregningModel.tidligereFakturertBeloep
        result.differansebeløp shouldBe BigDecimal(-1000)
    }

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

    private fun lagÅrsavregningModel(nyttTotalbeloep: BigDecimal, tidligereFakturertBeloep: BigDecimal): ÅrsavregningModel {
        val endeligAvgift = listOf(lagEndeligTrygdeavgiftsperiode())
        val tidligereAvgift = listOf(lagTidligereTrygdeavgiftsperiode())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList());

        return ÅrsavregningModel(
            årsavregningID = 112,
            år = 2024,
            tilFaktureringBeloep = null,
            endeligAvgift = endeligAvgift,
            tidligereAvgift = tidligereAvgift,
            nyttGrunnlag = grunnlagMedlemskap,
            nyttTotalbeloep = nyttTotalbeloep,
            tidligereFakturertBeloep = tidligereFakturertBeloep,
            tidligereGrunnlag = grunnlagMedlemskap
        )
    }

    private fun lagEndeligTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(// Initialize primary properties using setters or directly if accessible
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
            trygdedekning = Trygdedekninger.FULL_DEKNING
        },

        grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        })
    }

    private fun lagTidligereTrygdeavgiftsperiode(): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 12, 31),
        trygdeavgiftsbeløpMd = Penger(BigDecimal(450)),
        trygdesats = BigDecimal(900),

        grunnlagInntekstperiode = Inntektsperiode().apply {
            avgiftspliktigMndInntekt = Penger(BigDecimal(2800), NOK.kode)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
        },

        // Initialize nested Medlemskapsperiode using apply
        grunnlagMedlemskapsperiode = Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.FULL_DEKNING
        },

        // Initialize nested SkatteforholdTilNorge using apply
        grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        })
    }
}
