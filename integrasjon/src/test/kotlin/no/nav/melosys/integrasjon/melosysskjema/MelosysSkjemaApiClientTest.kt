package no.nav.melosys.integrasjon.melosysskjema

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,
        GenericAuthFilterFactory::class,
        MelosysSkjemaApiWebClientConfig::class,
        MelosysSkjemaApiClient::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MelosysSkjemaApiClientTest(
    @Autowired private val melosysSkjemaApiClient: MelosysSkjemaApiClient,
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
    fun `hentUtsendtArbeidstakerSkjema - mottar skjemadata - blir mappet`() {
        val skjemaId = UUID.randomUUID()
        val responseJson = """
            {
              "arbeidstakersDel": {
                "utenlandsoppdraget": {
                  "utsendelsesLand": "SE",
                  "utsendelsePeriode": {
                    "fraDato": "2024-01-01",
                    "tilDato": "2024-12-31"
                  }
                }
              },
              "arbeidsgiversDel": null,
              "referanseId": "MEL-5CA141"
            }
        """.trimIndent()

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseJson)
                )
        )

        val resultat = melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId)

        resultat.shouldNotBeNull()

        wireMockServer.verify(
            WireMock.getRequestedFor(WireMock.urlPathEqualTo("/m2m/api/skjema/utsendt-arbeidstaker/$skjemaId/data"))
                .withHeader("Authorization", WireMock.matching("Bearer .+"))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
        )
    }
}
