package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.mockk.spyk
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.springframework.mock.env.MockEnvironment
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ConsumerWireMockTestBase<T>(
    mockPort: Int,
    stsMockPort: Int
) {
    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    protected val stsMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(stsMockPort))

    open fun createWireMock(): MappingBuilder = WireMock.get(UrlPattern.ANY)

    abstract fun getMockData(): T

    abstract fun executeRequest()

    @BeforeAll
    fun beforeAll() {
        serviceUnderTestMockServer.start()
        stsMockServer.start()

        val environment = spyk(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        stsMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        serviceUnderTestMockServer.resetAll()
        stsMockServer.resetAll()
        defaultStsWireMockStub()
    }

    open fun defaultStsWireMockStub() {
        stsMockServer.stubFor(
            WireMock.get("/?grant_type=client_credentials&scope=openid").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"access_token\": \"--token-from-system--\", \"expires_in\": \"123\" }")
            )
        )
    }

    @AfterEach
    fun afterEach() {
        SpringSubjectHandler.set(NullSubjectHandler())
    }

    fun verifyHeaders(headers: Map<String, StringValuePattern>) {
        val wireMock = createWireMock()
        headers.forEach {
            wireMock.withHeader(it.key, it.value)
        }

        val response = WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")

        val data = getMockData()
        if (data is String) response.withBody(data)
        if (data is ByteArray) response.withBody(data)

        serviceUnderTestMockServer.stubFor(
            wireMock.willReturn(response)
        )
    }

    fun executeFromSystem(verify: () -> Unit) {
        val uuid = UUID.randomUUID()
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "prossesSteg")
            verify()
            executeRequest()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(uuid)
        }
    }

    fun executeFromController(verify: () -> Unit) {
        SpringSubjectHandler.set(TestSubjectHandler())
        try {
            ThreadLocalAccessInfo.beforeControllerRequest("request", false)
            verify()
            executeRequest()

        } finally {
            ThreadLocalAccessInfo.afterControllerRequest("request")
        }
    }

    fun executeErrorFromServer(verify: (String) -> Unit) {
        stubError()
        try {
            executeFromSystem { }
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

    open class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String? = "--token-from-user--"

        override fun getUserID(): String? = "Z123"
    }

    class NullSubjectHandler : TestSubjectHandler() {
        override fun getOidcTokenString(): String? = null
    }
}
