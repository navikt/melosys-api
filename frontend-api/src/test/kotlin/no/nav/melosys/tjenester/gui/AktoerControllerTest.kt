package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.aktoer.AktoerService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AktoerController::class])
class AktoerControllerTest {

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var aktoerService: AktoerService

    @MockkBean
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal opprette og oppdatere aktør`() {
        val aktoerDto = AktoerDto().apply {
            aktoerID = "1235"
            rolleKode = "BRUKER"
            orgnr = "123456789"
            databaseID = 1L
        }
        val fagsak = Fagsak.forTest { medVirksomhet() }

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { aktoerService.lagEllerOppdaterAktoer(any(), any()) } returns 1L
        every { aksesskontroll.autoriserSakstilgang(any<Fagsak>()) } returns Unit


        mockMvc.perform(
            post("/api/fagsaker/{saksnummer}/aktoerer", FagsakTestFactory.SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aktoerDto))
        )
            .andExpect(status().isOk)
            .andExpect(responseBody(objectMapper).containsObjectAsJson(aktoerDto, AktoerDto::class.java))
    }

    @Test
    fun `hentAktoer tilAktoerDto utenÅOppgiRolle`() {
        val aktoerMyndighet = Aktoer().apply {
            id = 39L
            aktørId = "1235"
            institusjonID = "INST2"
            rolle = Aktoersroller.TRYGDEMYNDIGHET
            orgnr = "100"
            fagsak = Fagsak.forTest { medVirksomhet() }
            utenlandskPersonId = "UTL"
        }

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns Fagsak.forTest { medVirksomhet() }
        every { aktoerService.hentfagsakAktører(any(), null) } returns listOf(aktoerMyndighet)
        every { aksesskontroll.autoriserSakstilgang(any<String>()) } returns Unit


        mockMvc.perform(
            get("/api/fagsaker/{saksnummer}/aktoerer", FagsakTestFactory.SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    listOf(AktoerDto.tilDto(aktoerMyndighet)),
                    object : TypeReference<List<AktoerDto>>() {}
                )
            )
    }

    @Test
    fun `hentAktoer tilAktoerDto OppgiRolle`() {
        val aktoerMyndighet = Aktoer().apply {
            id = 39L
            aktørId = "1235"
            institusjonID = "INST2"
            rolle = Aktoersroller.BRUKER
            orgnr = "100"
            fagsak = Fagsak.forTest { medVirksomhet() }
            utenlandskPersonId = "UTL"
        }

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns Fagsak.forTest { medVirksomhet() }
        every { aktoerService.hentfagsakAktører(any(), Aktoersroller.BRUKER) } returns listOf(aktoerMyndighet)
        every { aksesskontroll.autoriserSakstilgang(any<String>()) } returns Unit


        mockMvc.perform(
            get("/api/fagsaker/{saksnummer}/aktoerer", FagsakTestFactory.SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
                .param("rolleKode", "BRUKER")
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    listOf(AktoerDto.tilDto(aktoerMyndighet)),
                    object : TypeReference<List<AktoerDto>>() {}
                )
            )
    }

    @Test
    fun `slettAktoer ok`() {
        every { aktoerService.slettAktoer(123L) } returns Unit


        mockMvc.perform(delete("/api/fagsaker/aktoerer/{databaseID}", 123L))
            .andExpect(status().isNoContent)
    }
}
