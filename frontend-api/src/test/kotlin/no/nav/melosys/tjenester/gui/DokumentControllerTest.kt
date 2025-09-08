package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.service.dokument.DokumentHentingService
import no.nav.melosys.service.dokument.brev.SedPdfData
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [DokumentController::class])
class DokumentControllerTest {

    @MockkBean
    private lateinit var dokumentHentingService: DokumentHentingService

    @MockkBean
    private lateinit var eessiService: EessiService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal hente dokument`() {
        every { dokumentHentingService.hentJournalpost(any()) } returns Journalpost("jpID")
        val dokument = ByteArray(1)
        every { dokumentHentingService.hentDokument(any(), any()) } returns dokument


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "2", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `hentDokument journalpostMedFnr auditLogging`() {
        val journalpost = Journalpost("jpID").apply {
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            brukerId = "fnr"
            hoveddokument = ArkivDokument()
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)
        every { aksesskontroll.auditAutoriserFolkeregisterIdent(any(), any()) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "2", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)


        verify { aksesskontroll.auditAutoriserFolkeregisterIdent("fnr", any()) }
    }

    @Test
    fun `skal hente dokumenter oversikt`() {
        every { dokumentHentingService.hentJournalposter(any()) } returns emptyList()


        mockMvc.perform(
            get("$BASE_URL/oversikt/{saksnummer}", "1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `skal produsere utkast SED`() {
        val sedPdfData = SedPdfData()
        every { eessiService.genererSedPdf(any(), any<SedType>(), any()) } returns ByteArray(1)
        every { aksesskontroll.autoriser(any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/pdf/sed/utkast/{behandlingID}/{sedType}", 1L, SedType.A003)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sedPdfData))
        )
            .andExpect(status().isOk)


        verify { eessiService.genererSedPdf(any(), any<SedType>(), any()) }
    }

    companion object {
        private const val BASE_URL = "/api/dokumenter"
    }
}