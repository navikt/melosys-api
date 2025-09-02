package no.nav.melosys.tjenester.gui.saksflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.unntak.AnmodningUnntakService
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakDto
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakSvarDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AnmodningUnntakController::class])
class AnmodningUnntakControllerTest {

    @MockkBean
    private lateinit var anmodningUnntakService: AnmodningUnntakService
    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal anmode om unntak`() {
        val mottakerInstitusjon = "SE:321"
        val fritekstSed = "hei hei"
        val begrunnelseFritekst = "begrunnelse"
        
        val anmodningUnntakDto = AnmodningUnntakDto().apply {
            mottakerinstitusjon = mottakerInstitusjon
            this.fritekstSed = fritekstSed
            this.begrunnelseFritekst = begrunnelseFritekst
            val vedleggDto = VedleggDto("jpID", "dokID")
            vedlegg = setOf(vedleggDto)
        }
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { anmodningUnntakService.anmodningOmUnntak(any(), any(), any(), any(), any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/bestill", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anmodningUnntakDto))
        )
            .andExpect(status().isNoContent)


        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { 
            anmodningUnntakService.anmodningOmUnntak(
                BEHANDLING_ID, 
                mottakerInstitusjon,
                setOf(DokumentReferanse("jpID", "dokID")), 
                fritekstSed, 
                begrunnelseFritekst
            ) 
        }
    }

    @Test
    fun `skal svare`() {
        val anmodningUnntakSvarDto = AnmodningUnntakSvarDto("test")
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, "test") } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/svar", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anmodningUnntakSvarDto))
        )
            .andExpect(status().isNoContent)


        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, "test") }
    }

    companion object {
        private const val BASE_URL = "/api/saksflyt/anmodningsperioder"
        private const val BEHANDLING_ID = 3L
    }
}