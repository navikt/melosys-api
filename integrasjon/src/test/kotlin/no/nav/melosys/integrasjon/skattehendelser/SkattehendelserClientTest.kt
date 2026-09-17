package no.nav.melosys.integrasjon.skattehendelser

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        SkattehendelserWebClientConfig::class,
        SkattehendelserClient::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkattehendelserClientTest(
    @Autowired private val skattehendelserClient: SkattehendelserClient,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        wireMockServer.start()
        oAuthMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
        oAuthMockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        oAuthMockServer.reset()
        wireMockServer.resetAll()
    }

    @Test
    fun `hentSkattepliktige sender år og filtre som query-parametere og mapper svaret`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/admin/skattepliktige"))
                .withQueryParam("gjelderAar", WireMock.equalTo("2025"))
                .withQueryParam("aarFilter", WireMock.equalTo("INNTEKTSAAR"))
                .withQueryParam("publisertEtter", WireMock.equalTo("2026-09-08T00:00"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """
                            {
                              "gjelderAar": 2025,
                              "aarFilter": "INNTEKTSAAR",
                              "publisertEtter": "2026-09-08T00:00:00",
                              "antall": 1,
                              "skattepliktige": [
                                {
                                  "gjelderPeriode": "2025",
                                  "identifikator": "12345678901",
                                  "sisteHendelseTid": "2026-09-10T02:00:00",
                                  "inntektsaar": ["2024", "2025"],
                                  "antallPubliseringer": 2
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                )
        )

        val respons = skattehendelserClient.hentSkattepliktige(2025, ÅrFilter.INNTEKTSAAR, LocalDateTime.of(2026, 9, 8, 0, 0))

        respons shouldBe SkattepliktigeRespons(
            gjelderAar = 2025,
            antall = 1,
            skattepliktige = listOf(
                Skattepliktig(
                    gjelderPeriode = "2025",
                    identifikator = "12345678901",
                    sisteHendelseTid = LocalDateTime.of(2026, 9, 10, 2, 0),
                    inntektsaar = listOf("2024", "2025"),
                    antallPubliseringer = 2,
                )
            ),
        )
    }

    @Test
    fun `hentSkattepliktige sender FOM_AAR og token, og utelater publisertEtter når den ikke er satt`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/admin/skattepliktige"))
                .withQueryParam("aarFilter", WireMock.equalTo("FOM_AAR"))
                .withQueryParam("publisertEtter", WireMock.absent())
                .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .+"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"gjelderAar": 2025, "antall": 0, "skattepliktige": []}""")
                )
        )

        skattehendelserClient.hentSkattepliktige(2025, ÅrFilter.FOM_AAR, null).antall shouldBe 0
    }
}
