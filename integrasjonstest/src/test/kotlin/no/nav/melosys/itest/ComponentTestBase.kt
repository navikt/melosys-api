package no.nav.melosys.itest

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.Application
import no.nav.melosys.itest.mock.MelosysMockContainerConfig
import no.nav.melosys.itest.mock.MockVerificationClient
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local",
        "teammelosys.fattetvedtak.v1-local", "teammelosys.manglende-fakturabetaling-local", "teammelosys.melosys-hendelse-local",
        "teammelosys.skjema.innsendt.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@Import(KafkaTestConfig::class, KodeverkTestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
abstract class ComponentTestBase : OracleTestContainerBase() {

    @Autowired
    private lateinit var unleash: Unleash

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    val fakeUnleash: FakeUnleash by lazy {
        unleash.shouldBeInstanceOf<FakeUnleash>()
    }

    /**
     * Klient for verifisering av mock-tilstand via HTTP-endepunkter.
     */
    protected val mockVerificationClient: MockVerificationClient by lazy {
        MockVerificationClient(MelosysMockContainerConfig.getBaseUrl())
    }

    @BeforeEach
    fun componentTestBaseBeforeEach() {
        mockVerificationClient.clear()
    }

    @AfterEach
    fun componentTestBaseAfterEach() {
        fakeUnleash.enableAll()
    }

    val Any.toJsonNode: JsonNode
        get() = objectMapper.valueToTree(this)

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureMockProperties(registry: DynamicPropertyRegistry) {
            MelosysMockContainerConfig.configureProperties(registry)
        }
    }
}
