package no.nav.melosys.integrasjon.sak

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
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
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
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        CorrelationIdOutgoingFilter::class,
        SakClientConfig::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BasicAuthSakClientTest(
    @Autowired private val sakClient: SakClientInterface,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
        val environment = MockEnvironment().apply {
            setProperty("systemuser.username", "srvmelosys")
            setProperty("systemuser.password", "dummy")
        }
        EnvironmentHandler(environment)
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
    }

    @Test
    fun `opprettSak serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        """
                        {
                            "id": 123,
                            "tema": "MED",
                            "applikasjon": "MELOSYS",
                            "fagsakNr": "MEL-456",
                            "aktoerId": "12345678901",
                            "orgnr": null,
                            "opprettetAv": null,
                            "opprettetTidspunkt": null
                        }
                        """
                    )
            )
        )

        val request = SakDto(
            tema = "MED",
            applikasjon = "MELOSYS",
            saksnummer = "MEL-456",
            aktørId = "12345678901",
            orgnr = null,
        )

        sakClient.opprettSak(request)

        mockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/saker"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Basic .+"))
                .withRequestBody(
                    equalToJson(
                        """
                        {
                            "id": null,
                            "tema": "MED",
                            "applikasjon": "MELOSYS",
                            "fagsakNr": "MEL-456",
                            "aktoerId": "12345678901",
                            "orgnr": null,
                            "opprettetAv": null,
                            "opprettetTidspunkt": null
                        }
                        """,
                        true, false
                    )
                )
        )
    }
}
