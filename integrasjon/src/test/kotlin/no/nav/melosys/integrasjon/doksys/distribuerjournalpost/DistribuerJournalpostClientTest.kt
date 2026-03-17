package no.nav.melosys.integrasjon.doksys.distribuerjournalpost

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.arkiv.Distribusjonstidspunkt
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.Adresse
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.UUID

/**
 * Tester at DistribuerJournalpostClient:
 * - Sender korrekt JSON-body til dok-distribusjon-tjenesten
 * - Bruker Spring-konfigurerte beans (WebClient med Azure-auth) — samme oppsett som produksjonskode
 * - Deserialiserer response korrekt
 */
@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        DistribuerJournalpostClientConfig::class,
        DistribuerJournalpostClient::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DistribuerJournalpostClientTest(
    @Autowired private val distribuerJournalpostClient: DistribuerJournalpostClient,
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
    fun `distribuerJournalpost sender korrekt JSON-body`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"bestillingsId": "best-123"}""")
                )
        )

        val request = DistribuerJournalpostRequest.builder()
            .journalpostId("jp-456")
            .batchId("batch-789")
            .bestillendeFagsystem("MELOSYS")
            .dokumentProdApp("MELOSYS")
            .distribusjonstype(Distribusjonstype.VEDTAK)
            .distribusjonstidspunkt(Distribusjonstidspunkt.KJERNETID)
            .build()

        val response = distribuerJournalpostClient.distribuerJournalpost(request)

        response.bestillingsId shouldBe "best-123"

        mockServer.verify(
            postRequestedFor(urlEqualTo("/rest/v1/distribuerjournalpost"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "journalpostId": "jp-456",
                            "batchId": "batch-789",
                            "bestillendeFagsystem": "MELOSYS",
                            "dokumentProdApp": "MELOSYS",
                            "adresse": null,
                            "distribusjonstype": "VEDTAK",
                            "distribusjonstidspunkt": "KJERNETID"
                        }
                        """,
                        true, false
                    )
                )
        )
    }

    @Test
    fun `distribuerJournalpost serialiserer adresse korrekt`() {
        mockServer.stubFor(
            any(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"bestillingsId": "best-999"}""")
                )
        )

        val adresse = Adresse.builder()
            .adressetype("norskPostadresse")
            .adresselinje1("Storgata 1")
            .postnummer("0001")
            .poststed("Oslo")
            .land("NO")
            .build()

        val request = DistribuerJournalpostRequest.builder()
            .journalpostId("jp-789")
            .bestillendeFagsystem("MELOSYS")
            .dokumentProdApp("MELOSYS")
            .adresse(adresse)
            .distribusjonstype(Distribusjonstype.VIKTIG)
            .distribusjonstidspunkt(Distribusjonstidspunkt.UMIDDELBART)
            .build()

        distribuerJournalpostClient.distribuerJournalpost(request)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/rest/v1/distribuerjournalpost"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "journalpostId": "jp-789",
                            "batchId": null,
                            "bestillendeFagsystem": "MELOSYS",
                            "dokumentProdApp": "MELOSYS",
                            "adresse": {
                                "adresseType": "norskPostadresse",
                                "adresselinje1": "Storgata 1",
                                "adresselinje2": null,
                                "adresselinje3": null,
                                "postnummer": "0001",
                                "poststed": "Oslo",
                                "land": "NO"
                            },
                            "distribusjonstype": "VIKTIG",
                            "distribusjonstidspunkt": "UMIDDELBART"
                        }
                        """,
                        true, false
                    )
                )
        )
    }
}
