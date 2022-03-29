package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.pdl.*
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.*

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
internal class PDLConsumerIT(
    @Autowired private val server: MockRestServiceServer,
    @Autowired @Qualifier("system") private val pdlConsumer: PDLConsumer,
    @Value("\${mockserver.port}") private val mockPort: Int
) {
    val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    @TestConfiguration
    class TestConfig {
        val unleash = FakeUnleash()

        @Bean
        fun unleash(): Unleash {
            unleash.enableAll()
            return unleash
        }
    }

    class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String {
            return "--token-from-user--"
        }

        override fun getUserID(): String {
            throw IllegalStateException("getUserID skal ikke bli brukt av test")
        }
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

    @Test
    fun testRequestFromProsess() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")

        wireMockServer.stubFor(
            WireMock.post("/graphql")
                .withHeader("Authorization", WireMock.equalTo("Bearer --token-from-service--"))
                .withHeader("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-service--"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockData)
                )
        )

        pdlConsumer.hentIdenter("99026522600")
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun testRequestFraWeb() {
        SpringSubjectHandler.set(TestSubjectHandler())

        ThreadLocalAccessInfo.preHandle("request")

        wireMockServer.stubFor(
            WireMock.post("/graphql")
                .withHeader("Authorization", WireMock.equalTo("Bearer --token-from-user--"))
                .withHeader("Nav-Consumer-Token", WireMock.equalTo("Bearer --token-from-service--"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockData)
                )
        )

        pdlConsumer.hentIdenter("99026522600")

        ThreadLocalAccessInfo.afterCompletion("request")
    }

    private val mockData = """{
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
