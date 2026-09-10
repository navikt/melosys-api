package no.nav.melosys.tjenester.gui.kontroll

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Aksesstype
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
    fun `skal gi 400 uten å lekke detaljer når periodeFom er en ugyldig datostreng`() {
        postUnntaksperiode("""{"periodeFom": "Invalid date", "periodeTom": "2021-05-15"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .also { assertIngenInterneDetaljer(it) }

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når periodeTom er en ugyldig datostreng`() {
        postUnntaksperiode("""{"periodeFom": "2020-01-01", "periodeTom": "Invalid date"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .also { assertIngenInterneDetaljer(it) }

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når periodeFom er null`() {
        postUnntaksperiode("""{"periodeFom": null, "periodeTom": "2021-05-15"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .andExpect(jsonPath("$.feilkoder[0]").value(containsString("periodeFom")))
            .also { assertIngenInterneDetaljer(it) }

        // Bean-validering kjører før handler-metoden, så verken aksesskontroll eller tjenesten nås
        verify { aksesskontroll wasNot Called }
        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når periodeFom mangler helt`() {
        postUnntaksperiode("""{"periodeTom": "2021-05-15"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .andExpect(jsonPath("$.feilkoder[0]").value(containsString("periodeFom")))
            .also { assertIngenInterneDetaljer(it) }

        verify { aksesskontroll wasNot Called }
        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når periodeTom mangler helt`() {
        postUnntaksperiode("""{"periodeFom": "2020-01-01"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .andExpect(jsonPath("$.feilkoder[0]").value(containsString("periodeTom")))
            .also { assertIngenInterneDetaljer(it) }

        verify { aksesskontroll wasNot Called }
        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når periodeFom er et tall og ikke tolke det som epoch-day`() {
        postUnntaksperiode("""{"periodeFom": 12345, "periodeTom": "2021-05-15"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            .also { assertIngenInterneDetaljer(it) }

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal avvise YAML med 415 slik at tall som dato ikke slipper forbi via annet format`() {
        // Med YAML-converteren på plass ga dette 204 og datoen 2003-10-20
        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.parseMediaType("application/yaml"))
                .content("periodeFom: 12345\nperiodeTom: 2021-05-15\n"))
            .andExpect(status().isUnsupportedMediaType())

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 415 med Accept-header når Content-Type ikke er JSON`() {
        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.TEXT_PLAIN)
                .content("""{"periodeFom": "2020-01-01", "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isUnsupportedMediaType())
            // RFC 9110: 415 skal oppgi støttede formater
            .andExpect(header().string(HttpHeaders.ACCEPT, containsString(MediaType.APPLICATION_JSON_VALUE)))
            .also { assertIngenInterneDetaljer(it) }

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    @Test
    fun `skal gi 400 når behandlingID ikke er et tall`() {
        mockMvc.perform(post("$BASE_URL/abc/unntaksperiode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodeFom": "2020-01-01", "periodeTom": "2021-05-15"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UGYLDIG_FORESPØRSEL))
            // Springs egen melding her inneholder både typenavn og metodesignatur
            .also { assertIngenInterneDetaljer(it) }

        verify { unntaksperiodeKontrollService wasNot Called }
    }

    private fun postUnntaksperiode(body: String): ResultActions =
        mockMvc.perform(post("$BASE_URL/{behandlingID}/unntaksperiode", 22L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))

    private fun assertIngenInterneDetaljer(resultActions: ResultActions) {
        val body = resultActions.andReturn().response.contentAsString
        assertThat(body).doesNotContain("Invalid date")
        assertThat(body).doesNotContain("no.nav.melosys")
        assertThat(body).doesNotContain("org.springframework")
        assertThat(body).doesNotContain("java.lang")
        assertThat(body).doesNotContain("java.time")
    }

    companion object {
        private const val BASE_URL = "/api/kontroll"
        private const val UGYLDIG_FORESPØRSEL = "Ugyldig format på forespørselen"
    }
}
