package no.nav.melosys.tjenester.gui.fagsaker.trygdeavgift

import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [TrygdeavgiftFagsakController::class])
class TrygdeavgiftFagsakControllerTest {
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `hentTrygdeavgiftOppsummering skal returnere OK status og kalle nødvendige tjenester`() {
        every { aksesskontroll.autoriserSakstilgang(any<String>()) } returns Unit
        every { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(any<String>(), any<Boolean>()) } returns false


        mockMvc.perform(
            get("/api/fagsaker/{saksnr}/trygdeavgift/oppsummering", "123")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())


        verify { aksesskontroll.autoriserSakstilgang("123") }
        verify { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift("123", false) }
    }
}