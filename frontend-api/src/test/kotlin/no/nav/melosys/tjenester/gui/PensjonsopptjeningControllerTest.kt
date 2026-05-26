package no.nav.melosys.tjenester.gui

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.Unleash
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.featuretoggle.ToggleName
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

@WebMvcTest(controllers = [PensjonsopptjeningController::class])
internal class PensjonsopptjeningControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var pensjonsopptjeningOppslag: PensjonsopptjeningOppslag

    @MockkBean
    private lateinit var unleash: Unleash

    @BeforeEach
    fun setUp() {
        SpringSubjectHandler.set(TestSubjectHandler())
        every { aksesskontroll.autoriser(any()) } returns Unit
    }

    @Test
    fun `autoriser kjores foer toggle-sjekk og toggle av gir 204`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_VIS_PENSJONSOPPTJENING_POPP) } returns false

        mockMvc.perform(
            get(BASE_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        verify(exactly = 1) { aksesskontroll.autoriser(BEH_ID) }
        verify(exactly = 0) { pensjonsopptjeningOppslag.hent(any()) }
    }

    @Test
    fun `toggle paa - returnerer pensjonsopptjening DTO uten behandletAr`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_VIS_PENSJONSOPPTJENING_POPP) } returns true
        every { pensjonsopptjeningOppslag.hent(BEH_ID) } returns Pensjonsopptjening(
            inntektsAr = 2024,
            perioder = listOf(
                PensjonsopptjeningPeriode(aar = 2024, pgi = 540000, kilde = "SKATT"),
                PensjonsopptjeningPeriode(aar = 2023, pgi = 510000, kilde = "SKATT"),
            ),
        )

        mockMvc.perform(
            get(BASE_URL, BEH_ID).contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inntektsAr").value(2024))
            .andExpect(jsonPath("$.behandletAr").doesNotExist())
            .andExpect(jsonPath("$.perioder.length()").value(2))
            .andExpect(jsonPath("$.perioder[0].aar").value(2024))
            .andExpect(jsonPath("$.perioder[0].pgi").value(540000))
            .andExpect(jsonPath("$.perioder[0].kilde").value("SKATT"))

        verify { aksesskontroll.autoriser(BEH_ID) }
    }

    @Test
    fun `toggle paa - tom liste fra POPP - returnerer tom perioder`() {
        every { unleash.isEnabled(ToggleName.MELOSYS_VIS_PENSJONSOPPTJENING_POPP) } returns true
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

    companion object {
        private const val BASE_URL = "/api/behandlinger/{behandlingID}/pensjonsopptjening"
        private const val BEH_ID = 1234L
    }
}
