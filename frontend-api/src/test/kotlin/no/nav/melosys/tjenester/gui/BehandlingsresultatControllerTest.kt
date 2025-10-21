package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.begrunnelse
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.behandling.AngiBehandlingsresultatService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.AngiBehandlingsresultattypeDto
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto
import no.nav.melosys.tjenester.gui.dto.LagreFritekstDto
import no.nav.melosys.tjenester.gui.dto.OppdaterUtfallRegistreringUnntakDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [BehandlingsresultatController::class])
class BehandlingsresultatControllerTest {

    @MockkBean
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockkBean
    private lateinit var angiBehandlingsresultatService: AngiBehandlingsresultatService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `hentBehandlingsresultat returnerer behandlingsresultat`() {
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.hentBehandlingsresultatMedKontrollresultat(any()) } returns behandlingsresultat
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}/resultat", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    BehandlingsresultatDto.av(behandlingsresultat),
                    BehandlingsresultatDto::class.java
                )
            )
    }

    @Test
    fun `oppdaterFritekster oppdaterer fritekster`() {
        val dto = LagreFritekstDto("innledning", "begrunnelse", "trygdeavgift")
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.oppdaterFritekster(any(), any(), any(), any()) } returns behandlingsresultat
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/resultat/fritekst", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    BehandlingsresultatDto.av(behandlingsresultat),
                    BehandlingsresultatDto::class.java
                )
            )
    }

    @Test
    fun `oppdaterUtfallRegistreringUnntak oppdaterer utfall`() {
        val dto = OppdaterUtfallRegistreringUnntakDto(Utfallregistreringunntak.DELVIS_GODKJENT)
        val behandlingsresultat = lagBehandlingsresultat()
        every { behandlingsresultatService.oppdaterUtfallRegistreringUnntak(any(), any()) } returns behandlingsresultat
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every { aksesskontroll.autoriserSkrivOgTilordnet(1L) } returns Unit


        mockMvc.perform(
            put("$BASE_URL/{behandlingID}/resultat/utfallregistreringunntak", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    BehandlingsresultatDto.av(behandlingsresultat),
                    BehandlingsresultatDto::class.java
                )
            )
    }

    @Test
    fun `angiBehandlingsresultattype angir behandlingsresultattype`() {
        val dto = AngiBehandlingsresultattypeDto(Behandlingsresultattyper.HENLEGGELSE)
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every {
            angiBehandlingsresultatService.oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(
                1L,
                Behandlingsresultattyper.HENLEGGELSE
            )
        } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/resultat/type", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isNoContent)
    }

    private fun lagBehandlingsresultat() = Behandlingsresultat.forTest {
        type = Behandlingsresultattyper.IKKE_FASTSATT
        begrunnelseFritekst = "Bruker har fått flyskrekk"
        innledningFritekst = "<p>Bruker har fått flyskrekk</>"
        nyVurderingBakgrunn = Nyvurderingbakgrunner.FEIL_I_BEHANDLING.kode
        begrunnelse(Henleggelsesgrunner.ANNET.kode)
        vedtakMetadata {
            vedtakstype = Vedtakstyper.KORRIGERT_VEDTAK
        }
        årsavregning {
            id = 11L
        }
    }

    companion object {
        private const val BASE_URL = "/api/behandlinger"
    }
}
