package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.sak.HenleggelseService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.HenleggelseDto
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [HenleggelseController::class])
class HenleggelseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var henleggelseService: HenleggelseService

    @Test
    fun `skal henlegge fagsak`() {
        val begrunnelseKode = Henleggelsesgrunner.OPPHOLD_UTL_AVLYST.kode
        val fritekst = "Dette er fritekst"
        val henleggelseDto = HenleggelseDto(fritekst, begrunnelseKode)
        val saksnummer = "123"
        every { aksesskontroll.autoriserSakstilgang(any<String>()) } returns Unit
        every { henleggelseService.henleggFagsakEllerBehandling(any<String>(), any<String>(), any<String>()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{saksnr}/henlegg", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(henleggelseDto))
        )
            .andExpect(status().isNoContent())


        verify { henleggelseService.henleggFagsakEllerBehandling(saksnummer, begrunnelseKode, fritekst) }
    }

    @Test
    fun `skal henlegge sak som bortfalt`() {
        val behandlingId = 123L
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling.forTest {
            this.fagsak = fagsak
        }
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { aksesskontroll.autoriserSkriv(any<Long>()) } returns Unit
        every { henleggelseService.henleggSakEllerBehandlingSomBortfalt(any<Long>()) } returns Unit


        mockMvc.perform(
            put("$BASE_URL/{behandlingId}/henlegg-som-bortfalt", behandlingId)
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN)
        )
            .andExpect(status().isNoContent())


        verify { henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingId) }
    }

    companion object {
        private const val BASE_URL = "/api/fagsaker"
    }
}
