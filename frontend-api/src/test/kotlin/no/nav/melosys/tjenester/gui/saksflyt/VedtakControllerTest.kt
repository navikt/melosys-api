package no.nav.melosys.tjenester.gui.saksflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [VedtakController::class])
class VedtakControllerTest {

    @MockkBean
    private lateinit var vedtaksfattingFasade: VedtaksfattingFasade

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper


    @Test
    fun `skal fatte vedtak henleggelse`() {
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) } returns Unit

        val dto = FattVedtakDto().apply {
            setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE)
            setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            setMottakerinstitusjoner(setOf("SE:4343"))
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNoContent())


        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) }
    }

    @Test
    fun `skal fatte vedtak FTRL henleggelse`() {
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) } returns Unit

        val dto = FattVedtakDto().apply {
            setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE)
            setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            setBegrunnelseFritekst("Begrunnelse")
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNoContent())


        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) }
    }

    @Test
    fun `skal fatte vedtak trygdeavtale henleggelse`() {
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) } returns Unit

        val dto = FattVedtakDto().apply {
            setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE)
            setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            setBegrunnelseFritekst("Begrunnelse")
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNoContent())


        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { vedtaksfattingFasade.fattVedtak(BEHANDLING_ID, any<FattVedtakRequest>()) }
    }

    @Test
    fun fattVedtak_dtoManglerBehandlingresultat_girException() {
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit

        val dto = FattVedtakDto().apply {
            setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("BehandlingsresultatTypeKode eller vedtakstype mangler.")))
    }

    @Test
    fun fattVedtak_dtoManglerVedtakstype_girException() {
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit

        val dto = FattVedtakDto().apply {
            setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE)
        }


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("BehandlingsresultatTypeKode eller vedtakstype mangler.")))
    }

    companion object {
        private const val BEHANDLING_ID = 3L
        private const val BASE_URL = "/api/saksflyt/vedtak"
    }
}
