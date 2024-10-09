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
    fun `mapÅrsavregning skal mappe til ÅrsavregningVedtaksbrev når det finnes en årsavregning`() {
        val brevbestilling = ÅrsavregningVedtakBrevBestilling.Builder()
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Hei Test"
            })
            .medBehandling(lagBehandling())
            .build()
        val behandlingsresultat = createBehandlingsresultat()

        val årsavregningModel = createÅrsavregningModel()
        every { årsavregningService.finnÅrsavregning(any()) } returns årsavregningModel

        val result = mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)

        result.shouldNotBeNull()
        behandlingsresultat.årsavregning.aar shouldBe result.årsavregningsår

        val expectedEndeligTrygdeavgift = listOf(
            Avgiftsperiode(
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
        )
        result.endeligTrygdeavgift shouldBe expectedEndeligTrygdeavgift

        val expectedForskuddsvisFakturertTrygdeavgift = listOf(
            Avgiftsperiode(
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
        )
        result.forskuddsvisFakturertTrygdeavgift shouldBe expectedForskuddsvisFakturertTrygdeavgift

        result.endeligTrygdeavgiftTotalbeløp shouldBe årsavregningModel.nyttTotalbeloep
        result.forskuddsvisFakturertTrygdeavgiftTotalbeløp shouldBe årsavregningModel.tidligereFakturertBeloep
        result.differansebeløp shouldBe BigDecimal(-1000)
        result.minimumsbeløpForFakturering shouldBe BigDecimal(100)
        result.pliktigMedlemskap shouldBe true
        result.eøsEllerTrygdeavtale shouldBe true
    }

    @Test
    fun `mapÅrsavregning kaster FunksjonellException når årsavregning ikke funnet`() {
        val brevbestilling = ÅrsavregningVedtakBrevBestilling.Builder()
            .medBehandling(lagBehandling())
            .build()
        val behandlingsresultat = createBehandlingsresultat()

        every { årsavregningService.finnÅrsavregning(any()) } returns null

        val exception = assertThrows<FunksjonellException> {
            mapper.mapÅrsavregning(brevbestilling, behandlingsresultat)
        }

        exception.message shouldBe "Finner ingen årsavregning for behandling ${brevbestilling.behandlingId}"
    }

    private fun createBehandlingsresultat(): Behandlingsresultat {
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

    private fun createÅrsavregningModel(): ÅrsavregningModel {
        val endeligAvgift = listOf(createTrygdeavgiftsperiodeEndelig())
        val tidligereAvgift = listOf(createTrygdeavgiftsperiodeTidligere())

        val grunnlagMedlemskap = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList());

        return ÅrsavregningModel(år = 2024, tilFaktureringBeloep = BigDecimal(1000), endeligAvgift = endeligAvgift, tidligereAvgift = tidligereAvgift, nyttGrunnlag = grunnlagMedlemskap, nyttTotalbeloep = BigDecimal(1000), tidligereFakturertBeloep = BigDecimal(2000), tidligereGrunnlag = grunnlagMedlemskap)
    }

    private fun createTrygdeavgiftsperiodeEndelig(): Trygdeavgiftsperiode {
        val periode = Trygdeavgiftsperiode()
        periode.periodeFra = LocalDate.of(2023, 1, 1)
        periode.periodeTil = LocalDate.of(2023, 12, 31)

        val penger = Penger()
        penger.verdi = BigDecimal(500)
        periode.trygdeavgiftsbeløpMd = penger

        periode.trygdesats = BigDecimal(1000)

        val inntektsperiode = Inntektsperiode()
        inntektsperiode.avgiftspliktigInntektMnd = Penger(BigDecimal(2800), NOK.kode)
        inntektsperiode.type = Inntektskildetype.INNTEKT_FRA_UTLANDET
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt = true
        periode.grunnlagInntekstperiode = inntektsperiode

        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.trygdedekning = Trygdedekninger.FULL_DEKNING
        periode.grunnlagMedlemskapsperiode = medlemskapsperiode

        val skatteforhold = SkatteforholdTilNorge()
        skatteforhold.skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        periode.grunnlagSkatteforholdTilNorge = skatteforhold

        return periode
    }

    private fun createTrygdeavgiftsperiodeTidligere(): Trygdeavgiftsperiode {
        val periode = Trygdeavgiftsperiode()
        periode.periodeFra = LocalDate.of(2023, 1, 1)
        periode.periodeTil = LocalDate.of(2023, 12, 31)

        val penger = Penger()
        penger.verdi = BigDecimal(450)
        periode.trygdeavgiftsbeløpMd = penger

        periode.trygdesats = BigDecimal(900)

        val inntektsperiode = Inntektsperiode()
        inntektsperiode.avgiftspliktigInntektMnd = Penger(BigDecimal(2800), NOK.kode)
        inntektsperiode.type = Inntektskildetype.INNTEKT_FRA_UTLANDET
        inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt = false
        periode.grunnlagInntekstperiode = inntektsperiode

        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.trygdedekning = Trygdedekninger.FULL_DEKNING
        periode.grunnlagMedlemskapsperiode = medlemskapsperiode

        val skatteforhold = SkatteforholdTilNorge()
        skatteforhold.skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        periode.grunnlagSkatteforholdTilNorge = skatteforhold

        return periode
    }
}
