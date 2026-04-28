package no.nav.melosys.tjenester.gui

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.service.avgift.EøsPensjonistTrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift.TrygdeavgiftController
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(controllers = [TrygdeavgiftController::class])
class TrygdeavgiftControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    @MockkBean
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @MockkBean
    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    @MockkBean
    private lateinit var eøsPensjonistTrygdeavgiftsBeregningService: EøsPensjonistTrygdeavgiftsberegningService

    private val BASE_URL = "/api/behandlinger/{behandlingID}/trygdeavgift"
    private val BEHANDLINGSRESULTAT_ID = 1L
    private val trygdeavgiftsperioder = lagTrygdeavgiftsperioder()

    @Test
    fun `skal hente beregnet trygdeavgift`() {
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { trygdeavgiftService.hentTrygdeavgiftsperioder(BEHANDLINGSRESULTAT_ID) } returns trygdeavgiftsperioder

        mockMvc.perform(get("$BASE_URL/beregning", 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectResponseBody(forventetBeregnetTrygdeavgiftDto())
    }

    @Test
    fun `skal beregne trygdeavgift`() {
        every { aksesskontroll.autoriserSkrivOgTilordnet(any()) } returns Unit

        val trygdeavgiftsgrunnlagDto = lagTrygdeavgiftsgrunnlagDto()

        every {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                any(),
                any<List<SkatteforholdTilNorge>>(),
                any<List<Inntektsperiode>>()
            )
        } returns trygdeavgiftsperioder

        mockMvc.perform(
            put("$BASE_URL/beregning", 1L)
                .content(objectMapper.writeValueAsString(trygdeavgiftsgrunnlagDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpectResponseBody(forventetBeregnetTrygdeavgiftDto())
    }

    @Test
    fun beregnEøsPensjonistTrygdeavgift() {
        every { aksesskontroll.autoriserSkrivOgTilordnet(any()) } returns Unit

        val trygdeavgiftsgrunnlagDto = lagTrygdeavgiftsgrunnlagDto()

        every {
            eøsPensjonistTrygdeavgiftsBeregningService.beregnOgLagreTrygdeavgift(
                any(),
                any<List<SkatteforholdTilNorge>>(),
                any<List<Inntektsperiode>>()
            )
        } returns trygdeavgiftsperioder

        mockMvc.perform(
            put("$BASE_URL/eos-pensjonist/beregning", 1L)
                .content(objectMapper.writeValueAsString(trygdeavgiftsgrunnlagDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpectResponseBody(forventetBeregnetTrygdeavgiftDto())
    }

    @Test
    fun `skal finne fakturamottaker`() {
        val MOTTAKER_NAVN = "Fornavn Etternavn"
        every { trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLINGSRESULTAT_ID) } returns MOTTAKER_NAVN
        every { aksesskontroll.autoriser(any()) } returns Unit

        mockMvc.perform(get("$BASE_URL/fakturamottaker", 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectResponseBody(FakturamottakerDto(MOTTAKER_NAVN))
    }

    @Test
    fun `skal hente trygdeavgiftsperioder`() {
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { trygdeavgiftService.hentTrygdeavgiftsperioder(BEHANDLINGSRESULTAT_ID) } returns trygdeavgiftsperioder

        mockMvc.perform(get(BASE_URL, 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpectResponseBody(trygdeavgiftsperioder.map { TrygdeavgiftsperiodeDto(it) })
    }

    @Test
    fun `trygdeavgiftsperiode med TJUEFEM_PROSENT_REGEL og sammenslåtte inntektskilder serialiseres korrekt`() {
        val periode = lagTrygdeavgiftsperiodeMedBeregningsregel()
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { trygdeavgiftService.hentTrygdeavgiftsperioder(BEHANDLINGSRESULTAT_ID) } returns setOf(periode)

        mockMvc.perform(get(BASE_URL, 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect { result ->
                val json = result.response.contentAsString
                assert(json.contains("\"beregningsregel\":\"TJUEFEM_PROSENT_REGEL\"")) {
                    "Forventet beregningsregel TJUEFEM_PROSENT_REGEL i responsen, men fikk: $json"
                }
                assert(json.contains("\"harSammenslåtteInntektskilder\":true")) {
                    "Forventet harSammenslåtteInntektskilder=true i responsen, men fikk: $json"
                }
            }
    }

    private fun forventetBeregnetTrygdeavgiftDto(): BeregnetTrygdeavgiftDto {
        return BeregnetTrygdeavgiftDto(
            trygdeavgiftsperioder.map { TrygdeavgiftsperiodeDto(it) },
            lagTrygdeavgiftsgrunnlagDto()
        )
    }

    private fun lagTrygdeavgiftsgrunnlagDto(): TrygdeavgiftsgrunnlagDto {
        val skatteforholdTilNorgeDtos = trygdeavgiftsperioder
            .map { it.grunnlagSkatteforholdTilNorge }
            .map { SkatteforholdTilNorgeDto(it!!) }

        val inntektskildeDtos = trygdeavgiftsperioder
            .map { it.grunnlagInntekstperiode }
            .map { InntektskildeDto(it!!) }

        return TrygdeavgiftsgrunnlagDto(skatteforholdTilNorgeDtos, inntektskildeDtos)
    }

    fun lagTrygdeavgiftsperioder(): Set<Trygdeavgiftsperiode> {
        val trygdeavgift = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.now()
            periodeTil = LocalDate.now().plusDays(10)
            trygdesats = BigDecimal.valueOf(7.9)
            trygdeavgiftsbeløpMd = BigDecimal.valueOf(10000.0)
            medlemskapsperiode = medlemskapsperiodeForTest {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            }
            grunnlagInntekstperiode {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigTotalinntekt = Penger(5000.0)
            }
            grunnlagSkatteforholdTilNorge {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }

        return setOf(trygdeavgift)
    }

    fun lagTrygdeavgiftsperiodeMedBeregningsregel(): Trygdeavgiftsperiode {
        val periode = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.now()
            periodeTil = LocalDate.now().plusDays(10)
            trygdesats = null
            trygdeavgiftsbeløpMd = BigDecimal.valueOf(5000.0)
            beregningsregel = Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
            medlemskapsperiode = medlemskapsperiodeForTest {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            }
            grunnlagInntekstperiode {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigTotalinntekt = Penger(5000.0)
            }
            grunnlagSkatteforholdTilNorge {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        // Grunnlag 1: INNTEKT_FRA_UTLANDET
        val inntekt1 = Inntektsperiode().apply {
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(5000.0)
        }
        periode.leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(
            trygdeavgiftsperiode = periode,
            inntektsperiode = inntekt1,
            skatteforhold = skatteforhold,
            medlemskapsperiode = periode.grunnlagMedlemskapsperiode,
        ))

        // Grunnlag 2: gjør harSammenslåtteInntektskilder = true (mer enn ett grunnlag)
        val inntekt2 = Inntektsperiode().apply {
            fomDato = LocalDate.now()
            tomDato = LocalDate.now()
            type = Inntektskildetype.ARBEIDSINNTEKT
            avgiftspliktigMndInntekt = Penger(3000.0)
        }
        periode.leggTilGrunnlag(TrygdeavgiftsperiodeGrunnlag(
            trygdeavgiftsperiode = periode,
            inntektsperiode = inntekt2,
            skatteforhold = skatteforhold,
            medlemskapsperiode = periode.grunnlagMedlemskapsperiode,
        ))

        return periode
    }

    private inline fun <reified T> ResultActions.andExpectResponseBody(expectedObject: T): ResultActions =
        this.apply {
            responseBody(objectMapper).containsObjectAsJson(expectedObject, T::class.java)
        }
}
