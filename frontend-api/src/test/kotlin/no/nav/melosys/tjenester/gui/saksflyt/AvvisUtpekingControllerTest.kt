package no.nav.melosys.tjenester.gui.saksflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingAvvisDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AvvisUtpekingController::class])
class AvvisUtpekingControllerTest {

    @MockkBean
    private lateinit var utpekingService: UtpekingService
    
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val BASE_URL = "/api/saksflyt/utpeking"
    }

    @Test
    fun `skal avvise utpeking`() {
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { utpekingService.avvisUtpeking(any(), any<UtpekingAvvis>()) } returns Unit

        val dto = UtpekingAvvisDto().apply {
            setFritekst("test")
            setNyttLovvalgsland("DK")
            setBegrunnelseUtenlandskMyndighet("test")
            setVilSendeAnmodningOmMerInformasjon(false)
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/avvis", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNoContent())


        verify { utpekingService.avvisUtpeking(any(), any<UtpekingAvvis>()) }
    }
}