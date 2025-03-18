package no.nav.melosys.integrasjon.azuread

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
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
import java.util.*

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,
        CorrelationIdOutgoingFilter::class,

        AzureAdConsumerProducer::class,
        GenericAuthFilterFactory::class,
        FakeUnleash::class
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzureAdConsumerTest(
    @Autowired private val azureAdConsumer: AzureAdConsumer,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int
) {
    private val processUUID = UUID.randomUUID()

    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServiceUnderTestPort))

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
        oAuthMockServer.start()
        oAuthMockServer.reset()
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        oAuthMockServer.stop()
    }


    @BeforeEach
    fun before() {
        serviceUnderTestMockServer.resetAll()
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prossesSteg")
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
//        MetricsTestConfig.clearMeterRegistry()
    }


    @Test
    fun `hent saksbehandler navn hvor ident finnes i graph`() {
        val ident = "Z123456"
        val saksbehandlerNavn = "Lokal Testbruker"
        val graphUsersResponse = """
        {
            "value" : [ {
                "givenName" : "Lokal",
                "surname" : "Testbruker"
            } ]
        }"""
        serviceUnderTestMockServer.stubFor(
            get("/graph/v1.0/users?\$filter=onPremisesSamAccountName%20eq%20'Z123456'&\$count=true&\$select=givenName,surname")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(graphUsersResponse)
                        .withHeader("Content-Type", "application/json")
                )
        )

        val saksbehandlerNavnResponse = azureAdConsumer.hentSaksbehandlerNavn(ident)

        saksbehandlerNavnResponse.shouldBe(saksbehandlerNavn)
    }

    @Test
    fun `hent saksbehandler navn hvis ident ikke finnes i graph`() {
        val ident = "Z123456"
        val graphUsersResponse = """
        {
            "value" : []
        }"""
        serviceUnderTestMockServer.stubFor(
            get("/graph/v1.0/users?\$filter=onPremisesSamAccountName%20eq%20'Z123456'&\$count=true&\$select=givenName,surname")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(graphUsersResponse)
                        .withHeader("Content-Type", "application/json")
                )
        )

        val saksbehandlerNavnResponse = azureAdConsumer.hentSaksbehandlerNavn(ident)

        saksbehandlerNavnResponse.shouldBe(null)
    }

    fun get(url: String): MappingBuilder =
        WireMock.get(url)
            .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
}
