package no.nav.melosys.featuretoggle

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.google.gson.GsonBuilder
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.ActivationStrategy
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FeatureToggle
import no.finn.unleash.repository.JsonToggleCollectionDeserializer
import no.finn.unleash.repository.ToggleCollection
import no.finn.unleash.util.UnleashConfig
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ByUserIdStrategyTest {
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        SubjectHandler.set(null)
    }

    @Test
    fun `unleash skal være enabled når brukerID finnes i SubjectHandler`() {
        val subjectHandler: SubjectHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(subjectHandler)
        every { subjectHandler.userID } returns SAKSBEHANDLER
        val byUserIdStrategy = ByUserIdStrategy()


        val enabled = byUserIdStrategy.isEnabled(mapOf("user" to SAKSBEHANDLER))


        enabled.shouldBeTrue()
    }

    @Test
    fun `unleash skal være enabled når brukerID finnes i ThreadLocalAccessInfo`() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "", SAKSBEHANDLER)
        val byUserIdStrategy = ByUserIdStrategy()


        val enabled = byUserIdStrategy.isEnabled(mapOf("user" to SAKSBEHANDLER))


        enabled.shouldBeTrue()
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun `unleash skal være disabled når brukerID ikke finnes i SubjectHandler eller ThreadLocalAccessInfo`() {
        val byUserIdStrategy = ByUserIdStrategy()


        val enabled = byUserIdStrategy.isEnabled(mapOf("user" to SAKSBEHANDLER))


        enabled.shouldBeFalse()
    }

    @Test
    @Disabled("Ustabil når den kjøres i github actions")
    fun `unleash skal være enabled når brukerID retuneres fra unleash api`() {
        val subjectHandler: SubjectHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(subjectHandler)
        every { subjectHandler.userID } returns SAKSBEHANDLER
        wireMockServer.stubFor(
            WireMock.get("/client/features")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(createApiResponseJson())
                )
        )
        wireMockServer.stubFor(
            WireMock.post("/client/register")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                )
        )
        val unleashConfig = UnleashConfig.builder()
            .appName("melosys")
            .unleashAPI("http://localhost:" + wireMockServer.port())
            .build()
        val unleash = DefaultUnleash(unleashConfig, ByUserIdStrategy())


        val enabled = unleash.isEnabled("melosys.toggle")


        enabled.shouldBeTrue()


    }

    fun createApiResponseJson(): String {
        val toggleCollection =
            ToggleCollection(
                listOf(
                    FeatureToggle(
                        "melosys.toggle",
                        true,
                        mutableListOf(
                            ActivationStrategy("byUserId", mapOf("user" to "Z123456"))
                        )
                    )
                )
            )

        val gson = GsonBuilder()
            .registerTypeAdapter(
                ToggleCollection::class.java, JsonToggleCollectionDeserializer()
            )
            .create()
        return gson.toJson(toggleCollection)
    }


    companion object {
        private const val SAKSBEHANDLER = "Z123456"
    }
}
