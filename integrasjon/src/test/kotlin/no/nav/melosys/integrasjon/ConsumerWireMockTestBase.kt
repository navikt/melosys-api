package no.nav.melosys.integrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.mockk.spyk
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.springframework.context.annotation.Import
import org.springframework.mock.env.MockEnvironment
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(
    CorrelationIdOutgoingFilter::class,
)
abstract class ConsumerWireMockTestBase<T, R>(
    mockPort: Int,
    stsMockPort: Int,
    private val oAuthMockServer: OAuthMockServer
) {

    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    protected val stsMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(stsMockPort))

    open fun createWireMock(): MappingBuilder = WireMock.get(UrlPattern.ANY)

    abstract fun getMockData(): T

    abstract fun executeRequest(): R

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
        stsMockServer.start()
        oAuthMockServer.start()

        val environment = spyk(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        stsMockServer.stop()
        oAuthMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        serviceUnderTestMockServer.resetAll()
        stsMockServer.resetAll()
        defaultStsWireMockStub()
    }

    @AfterEach
    fun afterBase() {
    }

    open fun defaultStsWireMockStub() {
        stsMockServer.stubFor(
            WireMock.post("/token").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"access_token\": \"--token-from-system--\", \"expires_in\": \"123\" }")
            )
        )
    }

    @AfterEach
    fun afterEach() {
    }

    fun verifyHeaders(headers: Map<String, StringValuePattern>) {
        val wireMock = createWireMock()
        headers.forEach {
            wireMock.withHeader(it.key, it.value)
        }
        setupWireMock(wireMock)
    }

    fun setupWireMock(
        wireMock: MappingBuilder = createWireMock(),
        data: T = getMockData(),
        response: ResponseDefinitionBuilder = WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
    ): MappingBuilder {

        if (data is String) response.withBody(data)
        if (data is ByteArray) response.withBody(data)

        serviceUnderTestMockServer.stubFor(
            wireMock.willReturn(response)
        )
        return wireMock
    }

    fun executeFromSystem(consumer: (R) -> Unit = {}) {
        val uuid = UUID.randomUUID()
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "prossesSteg")
            oAuthMockServer.reset()
            consumer(executeRequest())
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(uuid)
        }
    }

    fun executeFromController(consumer: (R) -> Unit = {}) {
        try {
            ThreadLocalAccessInfo.beforeControllerRequest("request", false)
            oAuthMockServer.reset()
            consumer(executeRequest())
        } finally {
            ThreadLocalAccessInfo.afterControllerRequest("request")
        }
    }

    fun executeErrorFromServer(verify: (String) -> Unit) {
        stubError()
        try {
            executeFromSystem()
        } catch (exception: Exception) {
            Assertions.assertThat(exception.message)
                .endsWith(errorFromServerMessage())
            verify(exception.message!!)
        }
    }

    open fun stubError() {
        serviceUnderTestMockServer.stubFor(
            createWireMock()
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"melding\": \"Internal Server Error\"}")
                )
        )
    }

    open fun errorFromServerMessage() = "500 INTERNAL_SERVER_ERROR - {\"melding\": \"Internal Server Error\"}"

    companion object {
        const val UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
    }
}
