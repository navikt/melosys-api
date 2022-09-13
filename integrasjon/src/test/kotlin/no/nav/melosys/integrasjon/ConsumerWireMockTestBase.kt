package no.nav.melosys.integrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import io.mockk.spyk
import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.mock.env.MockEnvironment
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(
    value = [
        CorrelationIdOutgoingInterceptor::class,
        CorrelationIdOutgoingFilter::class
    ]
)
abstract class ConsumerWireMockTestBase<T, R>(
    mockPort: Int,
    stsMockPort: Int
) {
    @MockBean
    private val tokenValidationContextHolder: TokenValidationContextHolder? = null

    private val serviceUnderTestMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    protected val stsMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(stsMockPort))

    protected val azureMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(stsMockPort + 1))

    open fun createWireMock(): MappingBuilder = WireMock.get(UrlPattern.ANY)

    abstract fun getMockData(): T

    abstract fun executeRequest(): R

    @BeforeAll
    fun beforeAll() {
        Mockito.`when`(tokenValidationContextHolder!!.tokenValidationContext)
            .thenReturn(tokenValidationContext("sub1"))

        serviceUnderTestMockServer.start()
        stsMockServer.start()
        azureMockServer.start()

        val environment = spyk(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    @AfterAll
    fun afterAll() {
        serviceUnderTestMockServer.stop()
        stsMockServer.stop()
        azureMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        serviceUnderTestMockServer.resetAll()
        stsMockServer.resetAll()
        defaultStsWireMockStub()
        azureWireMockStub()
    }

    protected fun tokenValidationContext(sub: String): TokenValidationContext {
        val expiry = LocalDateTime.now().atZone(ZoneId.systemDefault()).plusSeconds(60).toInstant()
        val jwt: JWT = PlainJWT(
            JWTClaimsSet.Builder()
                .subject(sub)
                .audience("thisapi")
                .issuer("someIssuer")
                .expirationTime(Date.from(expiry))
                .claim("jti", UUID.randomUUID().toString())
                .build()
        )
        val map: MutableMap<String, JwtToken> = HashMap()
        map["issuer1"] = JwtToken(jwt.serialize())
        return TokenValidationContext(map)
    }

    open fun azureWireMockStub() {
        azureMockServer.stubFor(
            WireMock.post("/oauth2/v2.0/token").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(tokenServerResponse)
            )
        )
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
        setupWireMock { wireMock ->
            headers.forEach {
                wireMock.withHeader(it.key, it.value)
            }
        }
    }

    fun setupWireMock(consumer: (MappingBuilder) -> Unit = {}) {
        val wireMock = createWireMock()

        consumer(wireMock)

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

    fun executeFromSystem(consumer: (R) -> Unit = {}) {
        val uuid = UUID.randomUUID()
        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "prossesSteg")
            consumer(executeRequest())
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(uuid)
        }
    }

    fun executeFromController(consumer: (R) -> Unit = {}) {
        SpringSubjectHandler.set(TestSubjectHandler())
        try {
            ThreadLocalAccessInfo.beforeControllerRequest("request", false)
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

    open class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String? = "--token-from-user--"

        override fun getUserID(): String? = "Z123"
        override fun getName(): String? {
            return "Testy test"
        }

        override fun getGroups(): MutableList<String>? = ArrayList<String>()
    }

    class NullSubjectHandler : TestSubjectHandler() {
        override fun getOidcTokenString(): String? = null
        override fun getName(): String? = null
        override fun getGroups(): MutableList<String>? = null
    }

    companion object {
        const val UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"

        const val tokenServerResponse = """ {
        "token_type": "Bearer",
        "scope": "api://dev-fss.team-rocket.medlemskap-medl-api-q1/defaultaccess api://dev-fss.team-rocket.medlemskap-medl-api-q1/.default",
        "expires_in": 3952,
        "ext_expires_in": 3952,
        "access_token": "-- access_token -- ",
        "refresh_token": "-- refresh_token --"
        }
    """
    }

}
