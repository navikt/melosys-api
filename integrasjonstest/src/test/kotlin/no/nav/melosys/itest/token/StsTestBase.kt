package no.nav.melosys.itest.token

import no.nav.melosys.integrasjon.felles.EnvironmentHandler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class StsTestBase<T>(
    private val mockRestServerProvider: MockRestServerProvider
) {
    open class TestSubjectHandler : SubjectHandler() {
        override fun getOidcTokenString(): String? = "--token-from-user--"

        override fun getUserID(): String {
            throw IllegalStateException("getUserID skal ikke bli brukt av test")
        }
    }

    class NullSubjectHandler : TestSubjectHandler() {
        override fun getOidcTokenString(): String? = null
    }

    fun executeFromSystem(verify: () -> Unit) {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "prossesSteg")
        verify()
        executeRequest()
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    abstract fun stubError()

    fun executeErrorFromServer(verify: (String) -> Unit) {
        stubError()
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

    @BeforeAll
    fun beforeAll() {
        val environment = Mockito.spy(MockEnvironment())
        environment.setProperty("systemuser.username", "test")
        environment.setProperty("systemuser.password", "test")
        EnvironmentHandler(environment)
    }

    @BeforeEach
    fun setup() {
        mockRestServerProvider.reset()
        getSecurityMock().expect(requestTo("/?grant_type=client_credentials&scope=openid"))
            .andRespond(
                withSuccess(
                    "{ \"access_token\": \"--token-from-system--\", \"expires_in\": \"123\" }",
                    MediaType.APPLICATION_JSON
                )
            )
    }

    private fun getSecurityMock(): MockRestServiceServer {
        return mockRestServerProvider.getSecurityMock()
    }

    @AfterEach
    fun afterEach() {
        SpringSubjectHandler.set(NullSubjectHandler())
    }
}
