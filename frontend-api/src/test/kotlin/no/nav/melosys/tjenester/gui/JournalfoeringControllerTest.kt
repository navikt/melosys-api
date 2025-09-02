package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto
import no.nav.melosys.service.oppgave.OppgaveService
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [JournalfoeringController::class])
class JournalfoeringControllerTest {

    @MockkBean
    private lateinit var journalføringService: JournalfoeringService
    
    @MockkBean
    private lateinit var oppgaveService: OppgaveService

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var random: EasyRandom

    @BeforeEach
    fun setUp() {
        random = EasyRandom(EasyRandomParameters().collectionSizeRange(1, 4))
    }

    @Test
    fun `journalførOgKnyttTilSak skal validere kall`() {
        val journalføringDto = random.nextObject(JournalfoeringTilordneDto::class.java)
        every { journalføringService.journalførOgKnyttTilEksisterendeSak(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/knytt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførOgKnyttTilEksisterendeSak(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) }
    }

    @Test
    fun `journalførOgOpprettAndregangsBehandling skal validere kall`() {
        val journalføringDto = random.nextObject(JournalfoeringTilordneDto::class.java)
        every { journalføringService.journalførOgOpprettAndregangsBehandling(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/nyvurdering")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførOgOpprettAndregangsBehandling(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) }
    }

    @Test
    fun `journalføringOpprett skal validere kall`() {
        val journalføringDto = random.nextObject(JournalfoeringOpprettDto::class.java).apply {
            virksomhetOrgnr = null
            brukerID = SAMPLE_FNR
            behandlingstemaKode = Behandlingstema.ARBEID_FLERE_LAND.kode
        }
        every { journalføringService.journalførOgOpprettSak(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførOgOpprettSak(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) }
    }

    @Test
    fun `journalføringOpprett skal validere kall med fullmektigID null`() {
        val journalføringDto = random.nextObject(JournalfoeringOpprettDto::class.java).apply {
            virksomhetOrgnr = null
            brukerID = SAMPLE_FNR
            behandlingstemaKode = Behandlingstema.ARBEID_FLERE_LAND.kode
        }
        every { journalføringService.journalførOgOpprettSak(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførOgOpprettSak(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) }
    }

    @Test
    fun `journalføringOpprett skal validere kall med brukerID null`() {
        val journalføringDto = random.nextObject(JournalfoeringOpprettDto::class.java).apply {
            virksomhetOrgnr = SAMPLE_ORGNR
            brukerID = null
            behandlingstemaKode = Behandlingstema.ARBEID_FLERE_LAND.kode
        }
        every { journalføringService.journalførOgOpprettSak(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførOgOpprettSak(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringDto.oppgaveID) }
    }

    @Test
    fun `journalførSed skal validere kall`() {
        val journalføringSedDto = JournalfoeringSedDto().apply {
            oppgaveID = "123123"
            brukerID = SAMPLE_FNR
            journalpostID = "1231231232"
        }
        every { journalføringService.journalførSed(any()) } returns Unit
        every { oppgaveService.ferdigstillOppgave(journalføringSedDto.oppgaveID) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/sed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journalføringSedDto))
        ).andExpect(status().isNoContent())


        verify { journalføringService.journalførSed(any()) }
        verify { oppgaveService.ferdigstillOppgave(journalføringSedDto.oppgaveID) }
    }

    companion object {
        private const val SAMPLE_ORGNR = "899655123"
        private const val SAMPLE_FNR = "77777777772"
        private const val BASE_URL = "/api/journalforing"
    }
}