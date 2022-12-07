package no.nav.melosys.featuretoggle

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.google.gson.GsonBuilder
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
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
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ByUserIdStrategyTest {
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
    private val listAppender = ListAppender<ILoggingEvent>()

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        val logger = LoggerFactory.getLogger("no.nav.melosys.featuretoggle.ByUserIdStrategy") as Logger
        listAppender.context = LoggerFactory.getILoggerFactory() as LoggerContext
        logger.level = Level.WARN
        logger.addAppender(listAppender)
        listAppender.start()
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


        val enabled = byUserIdStrategy.isEnabled(mapOf("user" to "Z1,$SAKSBEHANDLER,Z2"))


        enabled.shouldBeTrue()
    }

    @Test
    fun `unleash skal være enabled når brukerID finnes i ThreadLocalAccessInfo`() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "", SAKSBEHANDLER, null)
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
        val unleash = setUpUnleashMock()

        val enabled = unleash.isEnabled("melosys.toggle")

        enabled.shouldBeTrue()
    }


    @Test
    @Disabled("Log sjekk fungere ikke i github actions")
    fun `det skal logges hvor unleash isEnabled blir kalt fra`() {
        val unleash = setUpUnleashMock(subjectHandlerReturnUserId = null)

        repeat((0..10).count()) {
            val enabled = unleash.isEnabled("melosys.toggle")
            enabled.shouldBeFalse()
        }

        listAppender.list.shouldHaveSize(1)
            .first().message.shouldContain(
                "Unleash byUserId Strategy brukes uten innlogget saksbehandler, blir kalt fra:\n" +
                    "no.nav.melosys.featuretoggle.ByUserIdStrategyTest.det skal logges hvor unleash isEnabled blir kalt fra"
            )
    }

    @Test // siden de andre testene ikke fungerer på github har vi denne i tillegg
    fun `unleash toggle skal bli false når vi ikke har saksbehandler`() {
        val unleash = setUpUnleashMock(subjectHandlerReturnUserId = null)

        val enabled = unleash.isEnabled("melosys.toggle")
        enabled.shouldBeFalse()
    }


    private fun setUpUnleashMock(subjectHandlerReturnUserId: String? = SAKSBEHANDLER): DefaultUnleash {
        val subjectHandler: SubjectHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(subjectHandler)
        every { subjectHandler.userID } returns subjectHandlerReturnUserId
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
        return DefaultUnleash(unleashConfig, ByUserIdStrategy())
    }

    private fun createApiResponseJson(): String {
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
