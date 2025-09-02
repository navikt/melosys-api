package no.nav.melosys.tjenester.gui.kontroll

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Aksesstype
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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

    companion object {
        private const val BASE_URL = "/api/kontroll"
    }
}