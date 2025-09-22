package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.VideresendSoknadService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.VideresendDto
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [VideresendingController::class])
class VideresendingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll
    
    @MockkBean
    private lateinit var fagsakService: FagsakService
    
    @MockkBean
    private lateinit var videresendSoknadService: VideresendSoknadService

    @Test
    fun `skal videresende`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val videresendDto = VideresendDto().apply {
            vedlegg = listOf(VedleggDto("1", "2"))
            mottakerinstitusjon = "Denmark"
            fritekst = "Dette er fritekst"
        }
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { aksesskontroll.autoriserSakstilgang(any<Fagsak>()) } returns Unit
        every { videresendSoknadService.videresend(any<String>(), any<String>(), any<String>(), any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{saksnr}/henlegg-videresend", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videresendDto))
        )
            .andExpect(status().isNoContent())
    }

    @Test
    fun `videresend uten vedlegg feiler`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val videresendDto = VideresendDto()
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { aksesskontroll.autoriserSakstilgang(any<Fagsak>()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{saksnr}/henlegg-videresend", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videresendDto))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("uten vedlegg")))
    }

    companion object {
        private const val BASE_URL = "/api/fagsaker"
    }
}