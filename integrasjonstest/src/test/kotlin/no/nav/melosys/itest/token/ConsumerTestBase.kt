package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ConsumerTestBase<T>(
    private val server: MockRestServiceServer,
    mockPort: Int
) {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    open class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String? = "--token-from-user--"

        override fun getUserID(): String {
            throw IllegalStateException("getUserID skal ikke bli brukt av test")
        }
    }

    class NullSubjectHandler : TestSubjectHandler() {
        override fun getOidcTokenString(): String? = null
    }

    open fun createWireMock(): MappingBuilder = WireMock.get(UrlPattern.ANY)

    abstract fun getMockData(): T

    fun executeFromSystem(verify: () -> Unit) {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "prossesSteg")
        verify()
        executeRequest()
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    fun executeErrorFromServer(verify: (String) -> Unit) {
        wireMockServer.stubFor(
            createWireMock()
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"melding\": \"Internal Server Error\"}")
                )
        )

        try {
            executeRequest()
        } catch (exception: Exception) {
            assertThat(exception.message)
                .endsWith("500 INTERNAL_SERVER_ERROR - {\"melding\": \"Internal Server Error\"}")
            verify(exception.message!!)
        }
    }

    fun executeFromController(verify: () -> Unit) {
        SpringSubjectHandler.set(TestSubjectHandler())
        ThreadLocalAccessInfo.beforeControllerRequest("request", false)
        verify()
        executeRequest()
        ThreadLocalAccessInfo.afterControllerRequest("request")
    }

    abstract fun executeRequest()

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

        wireMockServer.stubFor(
            wireMock.willReturn(response)
        )
    }

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()

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
        getSecurityMock().reset()
        getSecurityMock().expect(requestTo("/?grant_type=client_credentials&scope=openid"))
            .andRespond(
                withSuccess(
                    "{ \"access_token\": \"--token-from-system--\", \"expires_in\": \"123\" }",
                    MediaType.APPLICATION_JSON
                )
            )
    }

    open fun getSecurityMock(): MockRestServiceServer {
        return server
    }

    @AfterEach
    fun afterEach() {
        SpringSubjectHandler.set(NullSubjectHandler())
    }
}
