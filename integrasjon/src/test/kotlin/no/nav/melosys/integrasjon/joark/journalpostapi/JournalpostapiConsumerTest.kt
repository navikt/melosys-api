package no.nav.melosys.integrasjon.joark.journalpostapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.LogiskVedleggRequest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalpostapiConsumerTest {

    private lateinit var journalpostapiConsumer: JournalpostapiConsumer
    private lateinit var wireMockServer: WireMockServer

    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

    @BeforeAll
    fun `initial setup`() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val webClient = WebClient.builder()
            .baseUrl(wireMockServer.baseUrl())
            .build()

        journalpostapiConsumer = JournalpostapiConsumer(webClient)
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()

        wireMockServer.stubFor(any(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{}")))
    }

    @Test
    fun `opprettJournalpost - verifiser URL`() {
        val req = OpprettJournalpostRequest.OpprettJournalpostRequestBuilder()
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build()

        journalpostapiConsumer.opprettJournalpost(req, true)

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/journalpost"))
                .withQueryParam("forsoekFerdigstill", equalTo("true"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(matchingJsonPath("$.journalpostType", equalTo("INNGAAENDE")))
        )
    }

    @Test
    fun `oppdaterJournalpost - verifiser URL`() {
        val journalpostId = "123123"
        val oppdaterJournalpostRequest = OppdaterJournalpostRequest.Builder().medTittel("Tittel").build()

        journalpostapiConsumer.oppdaterJournalpost(oppdaterJournalpostRequest, journalpostId)

        wireMockServer.verify(
            putRequestedFor(urlPathEqualTo("/journalpost/$journalpostId"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(oppdaterJournalpostRequest)))
        )
    }

    @Test
    fun `leggTilLogiskVedlegg - verifiser URL`() {
        val dokumentInfoId = "532"
        val tittel = "tittel"
        journalpostapiConsumer.leggTilLogiskVedlegg(dokumentInfoId, tittel)

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokumentInfo/$dokumentInfoId/logiskVedlegg/"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson(
                    objectMapper.writeValueAsString(LogiskVedleggRequest(tittel))
                ))
        )
    }

    @Test
    fun `fjernLogiskeVedlegg - verifiser URL`() {
        val dokumentInfoId = "124j"
        val logiskVedleggId = "3j2io"

        journalpostapiConsumer.fjernLogiskeVedlegg(dokumentInfoId, logiskVedleggId)

        wireMockServer.verify(
            deleteRequestedFor(urlPathEqualTo("/dokumentInfo/$dokumentInfoId/logiskVedlegg/$logiskVedleggId"))
        )
    }

    @Test
    fun `ferdigstillJournalpost - verifiser URL`() {
        val journalpostId = "54325"
        val ferdigstillJournalpostRequest = FerdigstillJournalpostRequest()
        journalpostapiConsumer.ferdigstillJournalpost(ferdigstillJournalpostRequest, journalpostId)

        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/journalpost/$journalpostId/ferdigstill"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(ferdigstillJournalpostRequest)))
        )
    }

    @Test
    fun `opprettJournalpost - verifiser datoMottatt`() {
        val datoMottatt = "1970-01-01"

        val req = OpprettJournalpostRequest.OpprettJournalpostRequestBuilder()
            .datoMottatt(LocalDate.parse(datoMottatt))
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build()

        journalpostapiConsumer.opprettJournalpost(req, true)

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/journalpost"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(req)))
        )
    }
}
