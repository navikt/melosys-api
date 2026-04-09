package no.nav.melosys.integrasjon.joark.journalpostapi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.LogiskVedleggRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        JournalpostapiClientConfig::class,
        JournalpostapiClient::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalpostapiClientTest(
    @Autowired private val journalpostapiClient: JournalpostapiClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
        oAuthMockServer.reset()
    }

    @Test
    fun `opprettJournalpost serialiserer request og URL korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"journalpostId": "jp-001", "journalstatus": "ENDELIG"}""")
                )
        )

        val req = OpprettJournalpostRequest.OpprettJournalpostRequestBuilder()
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build()

        journalpostapiClient.opprettJournalpost(req, true)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/journalpost?forsoekFerdigstill=true"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson("""{"journalpostType":"INNGAAENDE","avsenderMottaker":null,"bruker":null,"tema":null,"behandlingstema":null,"tittel":null,"kanal":null,"journalfoerendeEnhet":null,"eksternReferanseId":null,"tilleggsopplysninger":null,"sak":null,"dokumenter":null,"datoMottatt":null}""", true, false))
        )
    }

    @Test
    fun `oppdaterJournalpost serialiserer request og URL korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")
                )
        )

        val journalpostId = "123123"
        val req = OppdaterJournalpostRequest.Builder().medTittel("Tittel").build()

        journalpostapiClient.oppdaterJournalpost(req, journalpostId)

        mockServer.verify(
            putRequestedFor(urlPathEqualTo("/journalpost/$journalpostId"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson("""{"datoMottatt":null,"tittel":"Tittel","journalfoerendeEnhet":"4530","bruker":null,"avsenderMottaker":null,"dokumenter":[],"sak":null,"tema":null}""", true, false))
        )
    }

    @Test
    fun `leggTilLogiskVedlegg serialiserer request og URL korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")
                )
        )

        val dokumentInfoId = "532"
        val tittel = "tittel"
        journalpostapiClient.leggTilLogiskVedlegg(dokumentInfoId, tittel)

        mockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokumentInfo/$dokumentInfoId/logiskVedlegg/"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson("""{"tittel":"tittel"}""", true, false)
                )
        )
    }

    @Test
    fun `fjernLogiskeVedlegg sender korrekt URL`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")
                )
        )

        val dokumentInfoId = "124j"
        val logiskVedleggId = "3j2io"

        journalpostapiClient.fjernLogiskeVedlegg(dokumentInfoId, logiskVedleggId)

        mockServer.verify(
            deleteRequestedFor(urlPathEqualTo("/dokumentInfo/$dokumentInfoId/logiskVedlegg/$logiskVedleggId"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
        )
    }

    @Test
    fun `ferdigstillJournalpost serialiserer request og URL korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{}")
                )
        )

        val journalpostId = "54325"
        val req = FerdigstillJournalpostRequest()
        journalpostapiClient.ferdigstillJournalpost(req, journalpostId)

        mockServer.verify(
            patchRequestedFor(urlPathEqualTo("/journalpost/$journalpostId/ferdigstill"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson("""{"journalfoerendeEnhet":"4530"}""", true, false))
        )
    }

    @Test
    fun `opprettJournalpost serialiserer datoMottatt korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"journalpostId": "jp-002", "journalstatus": "MOTTATT"}""")
                )
        )

        val datoMottatt = "1970-01-01"
        val req = OpprettJournalpostRequest.OpprettJournalpostRequestBuilder()
            .datoMottatt(LocalDate.parse(datoMottatt))
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build()

        journalpostapiClient.opprettJournalpost(req, true)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/journalpost?forsoekFerdigstill=true"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson("""{"journalpostType":"INNGAAENDE","avsenderMottaker":null,"bruker":null,"tema":null,"behandlingstema":null,"tittel":null,"kanal":null,"journalfoerendeEnhet":null,"eksternReferanseId":null,"tilleggsopplysninger":null,"sak":null,"dokumenter":null,"datoMottatt":"1970-01-01"}""", true, false))
        )
    }
}

