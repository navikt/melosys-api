package no.nav.melosys.tjenester.gui

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.service.popp.Pensjonsopptjening
import no.nav.melosys.service.popp.PensjonsopptjeningOppslag
import no.nav.melosys.service.popp.PensjonsopptjeningPeriode
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import no.nav.melosys.tjenester.gui.pensjonsopptjening.PensjonsopptjeningController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [PensjonsopptjeningController::class])
internal class PensjonsopptjeningControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var pensjonsopptjeningOppslag: PensjonsopptjeningOppslag

    @BeforeEach
    fun setUp() {
        SpringSubjectHandler.set(TestSubjectHandler())
        every { aksesskontroll.autoriser(any()) } returns Unit
    }

    @Test
    fun `returnerer pensjonsopptjening DTO med registrert og oppdatert som ISO-string`() {
        every { pensjonsopptjeningOppslag.hent(BEH_ID) } returns Pensjonsopptjening(
            inntektsAr = 2024,
            perioder = listOf(
                PensjonsopptjeningPeriode(
                    aar = 2024,
                    pgi = 540000,
                    kilde = "SKATT",
                    inntektType = "FL_PGI_NAERING",
                    inntektTypeDekode = "Pensjonsgivende inntekt av næring, frilanser",
                    registrert = LocalDate.of(2026, 5, 1),
                    oppdatert = LocalDate.of(2026, 5, 15),
                ),
                PensjonsopptjeningPeriode(
                    aar = 2024,
                    pgi = 200000,
                    kilde = "SKATT",
                    inntektType = "FL_PGI_LOENN",
                    inntektTypeDekode = null,
                    registrert = null,
                    oppdatert = null,
                ),
            ),
        )

        mockMvc.perform(
            get(BASE_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inntektsAr").value(2024))
            .andExpect(jsonPath("$.perioder.length()").value(2))
            .andExpect(jsonPath("$.perioder[0].aar").value(2024))
            .andExpect(jsonPath("$.perioder[0].pgi").value(540000))
            .andExpect(jsonPath("$.perioder[0].kilde").value("SKATT"))
            .andExpect(jsonPath("$.perioder[0].inntektType").value("FL_PGI_NAERING"))
            .andExpect(jsonPath("$.perioder[0].inntektTypeDekode").value("Pensjonsgivende inntekt av næring, frilanser"))
            .andExpect(jsonPath("$.perioder[0].registrert").value("2026-05-01"))
            .andExpect(jsonPath("$.perioder[0].oppdatert").value("2026-05-15"))
            .andExpect(jsonPath("$.perioder[1].inntektType").value("FL_PGI_LOENN"))
            .andExpect(jsonPath("$.perioder[1].inntektTypeDekode").isEmpty)
            .andExpect(jsonPath("$.perioder[1].registrert").isEmpty)
            .andExpect(jsonPath("$.perioder[1].oppdatert").isEmpty)

        verify { aksesskontroll.autoriser(BEH_ID) }
    }

    @Test
    fun `tom liste fra POPP - returnerer tom perioder`() {
        every { pensjonsopptjeningOppslag.hent(BEH_ID) } returns Pensjonsopptjening(
            inntektsAr = 2024,
            perioder = emptyList(),
        )

        mockMvc.perform(
            get(BASE_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.perioder.length()").value(0))
    }

    @Test
    fun `uautorisert - svarer 4xx og slaar ikke opp pensjonsopptjening`() {
        every { aksesskontroll.autoriser(BEH_ID) } throws SikkerhetsbegrensningException("Ikke tilgang")

        mockMvc.perform(
            get(BASE_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError())

        verify { aksesskontroll.autoriser(BEH_ID) }
        verify(exactly = 0) { pensjonsopptjeningOppslag.hent(any()) }
    }

    companion object {
        private const val BASE_URL = "/api/behandlinger/{behandlingID}/pensjonsopptjening"
        private const val BEH_ID = 1234L
    }
}
