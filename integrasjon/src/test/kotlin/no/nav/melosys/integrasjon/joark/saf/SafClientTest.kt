package no.nav.melosys.integrasjon.joark.saf

import tools.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.joark.saf.dto.HentDokumentoversiktResponse
import no.nav.melosys.integrasjon.joark.saf.dto.HentDokumentoversiktResponseWrapper
import no.nav.melosys.integrasjon.joark.saf.dto.SideInfo
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.AvsenderMottaker
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.AvsenderMottakerType
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Bruker
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Brukertype
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.DokumentInfo
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.DokumentVariant
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalposttype
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalstatus
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Sak
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
import java.util.Collections
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        SafClientProducer::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SafClientTest(
    @Autowired private val safClient: SafClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))
    private val objectMapper = JsonMapper.builder().build()

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
    fun `hentJournalpost serialiserer GraphQL request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(lagHentDokumentoversiktResponse("peker", false)))
            )
        )

        val expectedJson = """{"query":"  query(${'$'}journalpostId: String!) {\n    query: journalpost(journalpostId: ${'$'}journalpostId) {\n      journalpostId\n      tittel\n      journalstatus\n      tema\n      journalposttype\n      sak {\n        fagsakId\n      }\n      bruker {\n        id\n        type\n      }\n      avsenderMottaker {\n        id\n        type\n        navn\n        land\n      }\n      kanal\n      relevanteDatoer {\n        dato\n        datotype\n      }\n      dokumenter {\n        dokumentInfoId\n        tittel\n        brevkode\n        logiskeVedlegg {\n          logiskVedleggId\n          tittel\n        }\n        dokumentvarianter {\n          saksbehandlerHarTilgang\n          variantformat\n          filtype\n        }\n      }\n    }\n  }\n","variables":{"journalpostId":"jp-123"}}"""

        safClient.hentJournalpost("jp-123")

        mockServer.verify(
            postRequestedFor(urlEqualTo("/graphql"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(equalToJson(expectedJson, true, false))
        )
    }

    @Test
    fun `hentDokument når dokument finnes forvent PDF`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .withBody("pdf")
            )
        )

        val pdf = safClient.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)

        pdf shouldBe byteArrayOf('p'.code.toByte(), 'd'.code.toByte(), 'f'.code.toByte())
    }

    @Test
    fun `hentDokument når dokument finnes ikke 404 status kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(HENT_DOKUMENT_404_RESPONSE)
            )
        )

        val exception = shouldThrow<TekniskException> {
            safClient.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)
        }

        exception.message shouldContain "Kall mot SAF feilet."
    }

    @Test
    fun `hentDokument ikke autentisert kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(HENT_DOKUMENT_401_RESPONSE)
            )
        )

        val exception = shouldThrow<TekniskException> {
            safClient.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)
        }

        exception.message shouldContain "Kall mot SAF feilet."
    }

    @Test
    fun `hentJournalpost når journalpost finnes ikke kaster IntegrasjonException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE)
            )
        )

        val exception = shouldThrow<IntegrasjonException> {
            safClient.hentJournalpost(JOURNALPOST_ID)
        }

        exception.message shouldContain "ikke funnet"
    }

    @Test
    fun `hentJournalpost ikke autentisert kaster TekniskException`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(HENT_JOURNALPOST_401_RESPONSE)
            )
        )

        val exception = shouldThrow<TekniskException> {
            safClient.hentJournalpost(JOURNALPOST_ID)
        }

        exception.message shouldContain "Kall mot SAF feilet."
    }

    @Test
    fun `hentDokumentoversikt ingen paginering forvent antall journalposter`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(lagHentDokumentoversiktResponse("peker", false)))
            )
        )

        val journalposter = safClient.hentDokumentoversikt("MEL-1")

        journalposter shouldHaveSize 10
    }

    @Test
    fun `hentDokumentoversikt med paginering henter alle sider og returnerer totalt antall journalposter`() {
        mockServer.stubFor(
            any(anyUrl())
                .inScenario("paginering")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(lagHentDokumentoversiktResponse("cursor-side-2", true)))
                )
                .willSetStateTo("side2")
        )
        mockServer.stubFor(
            any(anyUrl())
                .inScenario("paginering")
                .whenScenarioStateIs("side2")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(lagHentDokumentoversiktResponse("cursor-side-3", false)))
                )
        )

        val journalposter = safClient.hentDokumentoversikt("MEL-1")

        journalposter shouldHaveSize 20
        mockServer.verify(moreThanOrExactly(2), postRequestedFor(urlEqualTo("/graphql")))
    }

    private fun lagHentDokumentoversiktResponse(nestePeker: String, finnesNeste: Boolean): GraphQLResponse<HentDokumentoversiktResponseWrapper> =
        GraphQLResponse(
            HentDokumentoversiktResponseWrapper(
                HentDokumentoversiktResponse(
                    lagJournalposter(10),
                    SideInfo(nestePeker, finnesNeste)
                )
            ), Collections.emptyList()
        )

    private fun lagJournalposter(antall: Int): List<Journalpost> =
        Stream.generate { lagJournalpost() }.limit(antall.toLong()).collect(Collectors.toList())

    private fun lagJournalpost(): Journalpost =
        Journalpost(
            "id",
            "tittel",
            Journalstatus.JOURNALFOERT,
            "MED",
            Journalposttype.I,
            Sak("123"),
            Bruker("123", Brukertype.FNR),
            AvsenderMottaker("123", AvsenderMottakerType.FNR, "navn", null),
            "SED",
            Collections.emptyList(),
            listOf(
                DokumentInfo(
                    "1",
                    "dok-tittel",
                    "brevkode",
                    Collections.emptyList(),
                    listOf(DokumentVariant(true, "ARKIV", "PDFA"))
                )
            )
        )

    companion object {
        private const val JOURNALPOST_ID = "1"
        private const val DOKUMENT_ID = "1"

        private const val HENT_DOKUMENT_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """

        private const val HENT_DOKUMENT_404_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 404,
          "error": "Not Found",
          "message": "Dokumentet tilnyttet journalpostId=1, dokumentInfoId=1, variant=ARKIV ikke funnet.",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """

        private const val HENT_JOURNALPOST_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/graphql"
        }
        """

        private const val HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE = """
        {
            "errors": [
                {
                    "message": "Journalpost med journalpostId=1 ikke funnet.",
                    "locations": [],
                    "extensions": {
                        "classification": "DataFetchingException"
                    }
                }
            ],
            "data": {
                "query": null
            }
        }
        """
    }
}
