package no.nav.melosys.tjenester.gui.fagsaker

import com.ninjasquad.springmockk.MockkBean
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import no.nav.melosys.service.sak.AnnullerSakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AnnulleringController::class])
@ExtendWith(MockKExtension::class)
class AnnulleringControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    lateinit var annullerSakService: AnnullerSakService

    @Test
    fun `verifisere kall mot annullerSakController`() {
        val saksnummer = "123456"

        justRun { aksesskontroll.autoriserSakstilgang(saksnummer) }
        justRun { annullerSakService.annullerSak(saksnummer) }

        mockMvc.perform(post("/api/fagsaker/{saksnummer}/annullering", saksnummer))
            .andExpect(status().isOk())
    }
}
