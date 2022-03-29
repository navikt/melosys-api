package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.pdl.PDLAuthFilter
import no.nav.melosys.integrasjon.pdl.PDLAuthFilterProducer
import no.nav.melosys.integrasjon.pdl.PDLConsumerImpl
import no.nav.melosys.integrasjon.pdl.PDLConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(
    value = [
        PDLConsumerImpl::class,
        PDLConsumerProducer::class,
        PDLAuthFilter::class,
        PDLAuthFilterProducer::class,
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class
    ],
    properties = ["spring.profiles.active:itest-aareg"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class PDLConsumerTestBase(
    private val server: MockRestServiceServer,
    mockPort: Int
) {
    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String {
            return "--token-from-user--"
        }

        override fun getUserID(): String {
            throw IllegalStateException("getUserID skal ikke bli brukt av test")
        }
    }

    fun verifyHeaders(headers: Map<String, StringValuePattern>) {
        val wireMock = WireMock.post("/graphql")
        headers.forEach {
            wireMock.withHeader(it.key, it.value)
        }

        wireMockServer.stubFor(
            wireMock.willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(mockData)
            )
        )
    }

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()

        server.expect(requestTo("/?grant_type=client_credentials&scope=openid"))
            .andRespond(
                withSuccess(
                    "{ \"access_token\": \"--token-from-service--\", \"expires_in\": \"123\" }",
                    MediaType.APPLICATION_JSON
                )
            )
        val environment = Mockito.spy(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
    }

    internal val mockData = """{
          "data": {
            "hentIdenter": {
              "identer": [
                {
                  "ident": "99026522600",
                  "gruppe": "FOLKEREGISTERIDENT"
                },
                {
                  "ident": "9834873315250",
                  "gruppe": "AKTORID"
                }
              ]
            }
          }
        }
        """
}
