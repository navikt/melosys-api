package no.nav.melosys.tjenester.gui

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.behandlingsnotatForTest
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.BehandlingsnotatService
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatPostDto
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [BehandlingsnotatController::class])
class BehandlingsnotatControllerTest {

    @MockkBean
    private lateinit var behandlingsnotatService: BehandlingsnotatService

    @MockkBean
    private lateinit var saksbehandlerService: SaksbehandlerService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `hentBehandlingsnotaterForFagsak returnerer notater`() {
        val saksnummer = "MEL-222"
        val behandlingsnotat = lagBehandlingsnotat()
        every { behandlingsnotatService.hentNotatForFagsak(saksnummer) } returns listOf(behandlingsnotat)
        every { behandlingsnotatService.kanRedigereNotat(any()) } returns true
        every { saksbehandlerService.finnNavnForIdent(SAKSBEHANDLER) } returns java.util.Optional.of(SAKSBEHANDLER)
        every { aksesskontroll.autoriserSakstilgang(saksnummer) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{saksnummer}/notater", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(1)))
            .andExpect(jsonPath("$[0].endretDato", equalTo(behandlingsnotat.endretDato.toString())))
            .andExpect(jsonPath("$[0].notatId", equalTo(behandlingsnotat.id.toInt())))
            .andExpect(jsonPath("$[0].registrertAvNavn", equalTo(behandlingsnotat.registrertAv)))
            .andExpect(jsonPath("$[0].tekst", equalTo(behandlingsnotat.tekst)))
    }

    @Test
    fun `oppdaterBehandlingsnotat oppdaterer notat`() {
        val saksnummer = "MEL-222"
        val dto = BehandlingsnotatPostDto("teteteksssst")
        val behandlingsnotat = lagBehandlingsnotat()
        every { behandlingsnotatService.oppdaterNotat(behandlingsnotat.id, any()) } returns behandlingsnotat
        every { behandlingsnotatService.kanRedigereNotat(any()) } returns true
        every { saksbehandlerService.finnNavnForIdent(SAKSBEHANDLER) } returns java.util.Optional.of(SAKSBEHANDLER)
        every { aksesskontroll.autoriserSakstilgang(saksnummer) } returns Unit


        mockMvc.perform(
            put("$BASE_URL/{saksnummer}/notater/{notatID}", saksnummer, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `opprettBehandlingsnotat oppretter nytt notat`() {
        val saksnummer = "MEL-222"
        val dto = BehandlingsnotatPostDto("teteteksssst")
        val behandlingsnotat = lagBehandlingsnotat()
        every { behandlingsnotatService.opprettNotat(saksnummer, any()) } returns behandlingsnotat
        every { behandlingsnotatService.kanRedigereNotat(any()) } returns true
        every { saksbehandlerService.finnNavnForIdent(SAKSBEHANDLER) } returns java.util.Optional.of(SAKSBEHANDLER)
        every { aksesskontroll.autoriserSakstilgang(saksnummer) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{saksnummer}/notater", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
    }

    private fun lagBehandlingsnotat() = behandlingsnotatForTest {
        tekst = "hei"
        registrertAv = SAKSBEHANDLER
        endretAv = SAKSBEHANDLER
        behandling {
            fagsak { }
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
        }
    }

    companion object {
        private const val SAKSBEHANDLER = "Z224234"
        private const val BASE_URL = "/api/fagsaker"
    }
}
