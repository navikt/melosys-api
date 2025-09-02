package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.tjenester.gui.dto.UtpekDto
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UtpekingController::class])
class UtpekingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll
    
    @MockkBean
    private lateinit var fagsakService: FagsakService
    
    @MockkBean
    private lateinit var utpekingService: UtpekingService

    @Test
    fun `skal utpeke lovvalgsland`() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val utpekDto = UtpekDto(setOf("SE:123"), "Fri SED", "Fri brev")
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { aksesskontroll.autoriserSakstilgang(any<Fagsak>()) } returns Unit
        every { utpekingService.utpekLovvalgsland(any<Fagsak>(), any<Set<String>>(), any<String>(), any<String>()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{saksnr}/utpek", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(utpekDto))
        )
            .andExpect(status().isNoContent())


        verify { aksesskontroll.autoriserSakstilgang(fagsak) }
        verify { utpekingService.utpekLovvalgsland(fagsak, utpekDto.mottakerinstitusjoner(), utpekDto.fritekstSed(), utpekDto.fritekstBrev()) }
    }

    companion object {
        private const val BASE_URL = "/api/fagsaker"
    }
}