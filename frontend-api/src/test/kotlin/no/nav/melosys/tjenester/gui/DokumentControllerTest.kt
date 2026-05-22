package no.nav.melosys.tjenester.gui

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
        every { dokumentHentingService.hentJournalpost(any()) } returns Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "2", tittel = "Vedtak")
        }
        val dokument = ByteArray(1)
        every { dokumentHentingService.hentDokument(any(), any()) } returns dokument


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `hentDokument journalpostMedFnr auditLogging`() {
        val journalpost = Journalpost("jpID").apply {
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            brukerId = "fnr"
            hoveddokument = ArkivDokument(dokumentId = "2")
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)
        val meldingSlot = slot<String>()
        every { aksesskontroll.auditAutoriserFolkeregisterIdent(any(), capture(meldingSlot)) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)


        verify { aksesskontroll.auditAutoriserFolkeregisterIdent("fnr", any()) }
        org.junit.jupiter.api.Assertions.assertTrue(meldingSlot.captured.contains("Dokument-2"), meldingSlot.captured)
    }

    @Test
    fun `hentDokument skal bruke dokumenttittel i Content-Disposition filnavn`() {
        val journalpost = Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "uuid-abc", tittel = "Vedtak om medlemskap")
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "uuid-abc")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Vedtak om medlemskap.pdf")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uuid-abc"))))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("attachment"))))
    }

    @Test
    fun `hentDokument skal falle tilbake til dokumentID-basert filnavn ved manglende tittel`() {
        val journalpost = Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "uuid-fallback", tittel = null)
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "uuid-fallback")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Dokument-uuid-fallback.pdf")))
    }

    @Test
    fun `hentDokument skal RFC 5987-encode norske tegn i filnavn`() {
        val journalpost = Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "uuid-aeoa", tittel = "Vedtak om medlemskap i æøå")
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "uuid-aeoa")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
    }

    @Test
    fun `hentDokument skal sanere path-separatorer i tittel`() {
        val journalpost = Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "uuid-path", tittel = "foo/bar\\baz")
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "uuid-path")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("foo/bar"))))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("foo\\bar"))))
    }

    @Test
    fun `hentDokument skal bruke vedleggets tittel naar dokumentID peker paa vedlegg`() {
        val journalpost = Journalpost("jpID").apply {
            hoveddokument = ArkivDokument(dokumentId = "hoved-id", tittel = "Hoveddokument")
            vedleggListe.add(ArkivDokument(dokumentId = "vedlegg-id", tittel = "Vedlegg tittel"))
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "vedlegg-id")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Vedlegg tittel.pdf")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Hoveddokument"))))
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

    @Test
    fun `hentDokument skal audit-logge tittelen til dokumentet som faktisk hentes`() {
        val journalpost = Journalpost("jpID").apply {
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            brukerId = "fnr"
            hoveddokument = ArkivDokument(dokumentId = "hoved-id", tittel = "Hoveddokument")
            vedleggListe.add(ArkivDokument(dokumentId = "vedlegg-id", tittel = "Vedlegg tittel"))
        }
        every { dokumentHentingService.hentJournalpost(any()) } returns journalpost
        every { dokumentHentingService.hentDokument(any(), any()) } returns ByteArray(1)
        val meldingSlot = slot<String>()
        every { aksesskontroll.auditAutoriserFolkeregisterIdent(any(), capture(meldingSlot)) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{journalpostID}/{dokumentID}", "1", "vedlegg-id")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)


        org.junit.jupiter.api.Assertions.assertTrue(meldingSlot.captured.contains("Vedlegg tittel"), meldingSlot.captured)
        org.junit.jupiter.api.Assertions.assertFalse(meldingSlot.captured.contains("Hoveddokument"), meldingSlot.captured)
    }

    companion object {
        private const val BASE_URL = "/api/dokumenter"
    }
}