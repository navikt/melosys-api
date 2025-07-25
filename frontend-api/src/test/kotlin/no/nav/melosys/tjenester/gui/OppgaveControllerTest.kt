package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.buildForTest
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.oppgave.OppgaveSoekFilter
import no.nav.melosys.service.oppgave.Oppgaveplukker
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import no.nav.melosys.tjenester.gui.dto.OppgaveSokDto
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto
import no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [OppgaveController::class])
internal class OppgaveControllerTest {
    @MockkBean
    private lateinit var oppgaveplukker: Oppgaveplukker

    @MockkBean
    private lateinit var oppgaveService: OppgaveService

    @MockkBean
    private lateinit var oppgaveSoekFilter: OppgaveSoekFilter

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun plukkOppgave() {
        val oppgave = Oppgave.Builder()
            .setOppgaveId("1")
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setSaksnummer("MEl-1")
            .setJournalpostId("123")
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .build()
        val innData = PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        val behandling = Behandling.buildForTest {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        }
        every { oppgaveplukker.plukkOppgave(any<String>(), any<PlukkOppgaveInnDto>()) } returns oppgave
        every { oppgaveService.hentSistAktiveBehandling(any<String>()) } returns behandling


        val expectedResponse = PlukketOppgaveDto(
            behandlingID = behandling.id,
            behandlingstype = behandling.type.kode,
            behandlingstema = behandling.tema.kode,
            journalpostID = oppgave.journalpostId,
            oppgaveID = oppgave.oppgaveId,
            saksnummer = oppgave.saksnummer
        )
        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/plukk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData))
        )
            .andExpect(status().isOk())
            .andExpect(
                responseBody(objectMapper)
                    .containsObjectAsJson(expectedResponse, PlukketOppgaveDto::class.java)
            )
    }

    @Test
    fun søkOppgaverMedPersonIdentEllerOrgnr_fnrSendesInn_kallerRettFunksjon() {
        val innData = OppgaveSokDto("fnr", null)
        every { oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent(any<String>()) } returns emptyList()

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData))
        )
            .andExpect(status().isOk())

        verify { oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent("fnr") }
        verify(exactly = 0) { oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr(any<String>()) }
    }

    @Test
    fun søkOppgaverMedPersonIdentEllerOrgnr_orgnrSendesInn_kallerRettFunksjon() {
        val innData = OppgaveSokDto(null, "orgnr")
        every { oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr(any<String>()) } returns emptyList()

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData))
        )
            .andExpect(status().isOk())

        verify { oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr("orgnr") }
        verify(exactly = 0) { oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent(any<String>()) }
    }

    @Test
    fun søkOppgaverMedPersonIdentEllerOrgnr_fnrOgOrgnrSendesInn_kasterFeil() {
        val innData = OppgaveSokDto("fnr", "orgnr")
        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData))
        )
            .andExpect(status().is4xxClientError())
            .andExpect(
                responseBody(objectMapper)
                    .containsError("message", "Fant både personIdent og orgnr. API støtter kun én.")
            )
    }

    @Test
    fun søkOppgaverMedPersonIdentEllerOrgnr_ingentingSendesInn_kasterFeil() {
        val innData = OppgaveSokDto(null, null)
        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(innData))
        )
            .andExpect(status().is4xxClientError())
            .andExpect(
                responseBody(objectMapper)
                    .containsError(
                        "message",
                        "Finner ingen søkekriteria. API støtter personIdent(fnr eller dnr) og orgnr"
                    )
            )
    }

    companion object {
        private const val BASE_URL = "/api/oppgaver"
    }
}
