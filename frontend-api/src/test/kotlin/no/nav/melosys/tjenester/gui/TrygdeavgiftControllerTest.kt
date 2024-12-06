package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift.TrygdeavgiftController
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*
import no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(controllers = [TrygdeavgiftController::class])
class TrygdeavgiftControllerTest {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    @MockkBean
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @MockkBean
    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val BASE_URL = "/api/behandlinger/{behandlingID}/trygdeavgift"
    private val BEHANDLINGSRESULTAT_ID = 1L
    private val trygdeavgiftsperioder = lagTrygdeavgiftsperioder()

    @Test
    fun hentTrygdeavgiftsperioder() {
        every { aksesskontroll.autoriser(any()) } returns Unit
        every { trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLINGSRESULTAT_ID) } returns trygdeavgiftsperioder

        mockMvc.perform(get("$BASE_URL/beregning", 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper)
                    .containsObjectAsJson(
                        forventetBeregnetTrygdeavgiftDto(),
                        BeregnetTrygdeavgiftDto::class.java
                    )
            )
    }

    @Test
    fun beregnTrygdeavgift() {
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
            .andExpect(
                responseBody(objectMapper)
                    .containsObjectAsJson(
                        forventetBeregnetTrygdeavgiftDto(),
                        BeregnetTrygdeavgiftDto::class.java
                    )
            )
    }

    @Test
    fun finnFakturamottaker() {
        val MOTTAKER_NAVN = "Fornavn Etternavn"
        every { trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLINGSRESULTAT_ID) } returns MOTTAKER_NAVN
        every { aksesskontroll.autoriser(any()) } returns Unit

        mockMvc.perform(get("$BASE_URL/fakturamottaker", 1L)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper)
                    .containsObjectAsJson(
                        FakturamottakerDto(MOTTAKER_NAVN),
                        FakturamottakerDto::class.java
                    )
            )
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
        val trygdeavgift = Trygdeavgiftsperiode(
            periodeFra = LocalDate.now(),
            periodeTil = LocalDate.now().plusDays(10),
            trygdesats = BigDecimal.valueOf(7.9),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(10000.0)),

            grunnlagMedlemskapsperiode = Medlemskapsperiode().apply {
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            },

            grunnlagInntekstperiode = Inntektsperiode().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigTotalinntekt = Penger(5000.0)
            },

            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now()
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        return setOf(trygdeavgift)
    }
}
