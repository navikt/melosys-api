package no.nav.melosys.tjenester.gui.kontroll

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Aksesstype
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(controllers = [ValideringUnntaksperiodeController::class])
class ValideringUnntaksperiodeControllerTest {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll
    @MockkBean
    private lateinit var unntaksperiodeKontrollService: UnntaksperiodeKontrollService

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal validere unntak OK`() {
        val requestDto = ValideringUnntaksperiodeController.UnntaksperiodeRequestDto(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-05-15"))
        every { aksesskontroll.autoriser(22L, Aksesstype.LES) } returns Unit
        every { unntaksperiodeKontrollService.kontrollPeriode(22L, requestDto.tilPeriode()) } returns Unit


        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isNoContent())


        verify { aksesskontroll.autoriser(22L, Aksesstype.LES) }
        verify { unntaksperiodeKontrollService.kontrollPeriode(22L, requestDto.tilPeriode()) }
    }

    @Test
    fun `skal gi 400 når periodeFom er en ugyldig datostreng`() {
        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeFom": "Invalid date", "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())

        verify(exactly = 0) { unntaksperiodeKontrollService.kontrollPeriode(any<Long>(), any()) }
    }

    @Test
    fun `skal ikke lekke detaljer fra parsefeil i responsen`() {
        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeFom": "Invalid date", "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Ugyldig format på forespørselen"))
    }

    @Test
    fun `skal gi 400 når periodeFom er null`() {
        every { aksesskontroll.autoriser(22L, Aksesstype.LES) } returns Unit

        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeFom": null, "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())

        verify(exactly = 0) { unntaksperiodeKontrollService.kontrollPeriode(any<Long>(), any()) }
    }

    @Test
    fun `skal gi 400 når periodeFom mangler helt`() {
        every { aksesskontroll.autoriser(22L, Aksesstype.LES) } returns Unit

        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())

        verify(exactly = 0) { unntaksperiodeKontrollService.kontrollPeriode(any<Long>(), any()) }
    }

    @Test
    fun `skal gi 400 når behandlingID ikke er et tall`() {
        mockMvc.perform(post("$BASE_URL/abc/unntaksperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeFom": "2020-01-01", "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())

        verify(exactly = 0) { unntaksperiodeKontrollService.kontrollPeriode(any<Long>(), any()) }
    }

    companion object {
        private const val BASE_URL = "/api/kontroll"
    }
}