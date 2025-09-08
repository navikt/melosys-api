package no.nav.melosys.tjenester.gui.kontroll

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.AnmodningUnntakKontrollService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.kontroll.feature.postadresse.PostadresseKontrollService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Aksesstype
import no.nav.melosys.tjenester.gui.dto.kontroller.AnmodningOmUnntakKontrollerDto
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [KontrollController::class])
class KontrollControllerTest {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll
    
    @MockkBean
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade
    
    @MockkBean
    private lateinit var eessiService: EessiService
    
    @MockkBean
    private lateinit var behandlingService: BehandlingService
    
    @MockkBean
    private lateinit var postadresseKontrollService: PostadresseKontrollService
    
    @MockkBean
    private lateinit var anmodningUnntakKontrollService: AnmodningUnntakKontrollService

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `kontrollerFerdigbehandling skal returnere ok`() {
        val dto = FerdigbehandlingKontrollerDto(
            1L, 
            Vedtakstyper.FØRSTEGANGSVEDTAK,
            Behandlingsresultattyper.HENLEGGELSE, 
            null, 
            false
        )
        every { aksesskontroll.autoriser(1L, Aksesstype.LES) } returns Unit
        every { ferdigbehandlingKontrollFacade.kontroller(1L, false, Behandlingsresultattyper.HENLEGGELSE, null) } returns emptyList()


        mockMvc.perform(
            post("$BASE_URL/ferdigbehandling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk())
    }

    @Test
    fun `kontrollerFerdigbehandling skal gi BadRequest når vedtakstype mangler`() {
        val dto = FerdigbehandlingKontrollerDto(
            1L, 
            null, 
            Behandlingsresultattyper.HENLEGGELSE,
            null, 
            false
        )


        mockMvc.perform(
            post("$BASE_URL/ferdigbehandling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Vedtakstype mangler.")))
    }

    @Test
    fun `kontrollerAnmodningOmUnntak skal returnere ok`() {
        val dto = AnmodningOmUnntakKontrollerDto(1L)
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { anmodningUnntakKontrollService.utførKontroller(1L) } returns emptyList()


        mockMvc.perform(
            post("$BASE_URL/anmodningomunntak")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk())
    }

    companion object {
        private const val BASE_URL = "/api/kontroll"
    }
}