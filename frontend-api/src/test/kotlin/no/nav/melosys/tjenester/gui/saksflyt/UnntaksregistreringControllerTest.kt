package no.nav.melosys.tjenester.gui.saksflyt

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.service.UnntaksregistreringService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UnntaksregistreringController::class])
class UnntaksregistreringControllerTest {

    @MockkBean
    private lateinit var unntaksregistreringService: UnntaksregistreringService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `skal registrere unntak fra medlemskap og returnere no content`() {
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every { unntaksregistreringService.registrerUnntakFraMedlemskap(1L) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent())


        verify { aksesskontroll.autoriserSkriv(1L) }
        verify { unntaksregistreringService.registrerUnntakFraMedlemskap(1L) }
    }

    companion object {
        private const val BASE_URL = "/api/saksflyt/unntaksregistrering"
    }
}
