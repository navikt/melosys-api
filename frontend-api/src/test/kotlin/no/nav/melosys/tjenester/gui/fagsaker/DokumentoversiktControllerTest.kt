package no.nav.melosys.tjenester.gui.fagsaker

import no.nav.melosys.service.dokument.DokumentHentingService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [DokumentoversiktController::class])
class DokumentoversiktControllerTest {
    @MockkBean
    private lateinit var dokumentHentingService: DokumentHentingService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `hentDokumenter skal returnere OK status`() {
        every { dokumentHentingService.hentJournalposter(any<String>()) } returns emptyList()


        mockMvc.perform(
            get("$BASE_URL/fagsaker/{saksnummer}/dokumenter", "1")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
    }

    companion object {
        private const val BASE_URL = "/api"
    }
}