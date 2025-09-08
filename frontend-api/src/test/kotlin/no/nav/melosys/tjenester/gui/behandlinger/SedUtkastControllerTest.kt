package no.nav.melosys.tjenester.gui.behandlinger

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.service.dokument.brev.SedPdfData
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [SedUtkastController::class])
class SedUtkastControllerTest {
    @MockkBean
    private lateinit var eessiService: EessiService
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal produsere utkast SED`() {
        val sedPdfData = SedPdfData()
        every { eessiService.genererSedPdf(any(), any<SedType>(), any()) } returns byteArrayOf(1)
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(post("$BASE_URL/behandlinger/{behandlingID}/sed/{sedType}/utkast", 1L, SedType.A003)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sedPdfData)))
            .andExpect(status().isOk())


        verify { eessiService.genererSedPdf(any(), any<SedType>(), any()) }
    }

    companion object {
        private const val BASE_URL = "/api"
    }
}