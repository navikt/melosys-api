package no.nav.melosys.itest.mock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.Application
import no.nav.melosys.itest.KafkaTestConfig
import no.nav.melosys.itest.OracleTestContainerBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Base class for integration tests that use melosys-mock as a Docker container
 * instead of the in-process mock.
 *
 * This class combines:
 * - OracleTestContainerBase for database
 * - Melosys-mock container for external service mocking
 * - All Spring Boot test configuration from ComponentTestBase
 *
 * Key differences from ComponentTestBase:
 * - External service URLs point to the container (dynamic port) instead of localhost:8093
 * - Mock state is cleared via HTTP endpoints, not direct repo access
 * - No in-process mock code is used
 *
 * Usage:
 * 1. Extend this class instead of ComponentTestBase
 * 2. Use mockVerificationClient to verify what was sent to mocks
 * 3. All external service calls go to the Docker container
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local",
        "teammelosys.fattetvedtak.v1-local", "teammelosys.manglende-fakturabetaling-local", "teammelosys.melosys-hendelse-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@Import(KafkaTestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
open class ContainerComponentTestBase : OracleTestContainerBase() {

    @Autowired
    private lateinit var unleash: Unleash

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    val fakeUnleash: FakeUnleash by lazy {
        unleash.shouldBeInstanceOf<FakeUnleash>()
    }

    /**
     * Client for verifying mock state via HTTP endpoints.
     * Points to the mock container.
     */
    protected val mockVerificationClient: MockVerificationClient by lazy {
        MockVerificationClient(getMockBaseUrl())
    }

    @BeforeEach
    fun containerComponentTestBaseBeforeEach() {
        // Clear mock state before each test
        mockVerificationClient.clear()
    }

    @AfterEach
    fun containerComponentTestBaseAfterEach() {
        // Clear mock state via HTTP (container doesn't have direct repo access)
        mockVerificationClient.clear()
        fakeUnleash.enableAll()
    }

    val Any.toJsonNode: JsonNode
        get() = objectMapper.valueToTree(this)

    companion object {
        private val log = LoggerFactory.getLogger(ContainerComponentTestBase::class.java)

        /**
         * Docker image from Google Artifact Registry.
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Internal port used by melosys-mock (server.port=8083 in the container).
         */
        private const val MOCK_PORT = 8083

        /**
         * The mock container instance. Shared across all tests using this base class.
         */
        val mockContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse(MOCK_IMAGE))
            .withExposedPorts(MOCK_PORT)
            .withLogConsumer(Slf4jLogConsumer(log).withPrefix("melosys-mock"))
            .waitingFor(
                Wait.forHttp("/actuator/health")
                    .forPort(MOCK_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2))
            )
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")

        /**
         * Get the base URL for the mock container.
         */
        fun getMockBaseUrl(): String {
            if (!mockContainer.isRunning) {
                throw IllegalStateException("Mock container is not running")
            }
            return "http://${mockContainer.host}:${mockContainer.getMappedPort(MOCK_PORT)}"
        }

        /**
         * Configure all external service URLs to point to the mock container.
         * This overrides the localhost:8093 URLs in application-test.yml.
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureMockAndDbProperties(registry: DynamicPropertyRegistry) {
            // Start the mock container if not already running
            if (!mockContainer.isRunning) {
                log.info("Starting melosys-mock container...")
                mockContainer.start()
                log.info("Mock container started at: ${getMockBaseUrl()}")
            }

            val mockUrl = getMockBaseUrl()

            // Configure all external service URLs to point to the mock container
            // These match the properties in application-test.yml

            // REST APIs
            // NOTE: Some APIs are NOT overridden and use the in-process mock at localhost:8093
            // because the container doesn't have these endpoints:
            // - KodeverkAPI_v1 (kodeverk endpoint)
            // - Inngangsvilkaar (inngangsvilkaar endpoint)
            registry.add("DistribuerJournalpost_v1.url") { "$mockUrl/rest/v1/distribuerjournalpost" }
            registry.add("JournalpostApi_v1.url") { "$mockUrl/rest/journalpostapi/v1" }
            registry.add("MelosysEessi.url") { "$mockUrl/api" }
            registry.add("OppgaveAPI_v1.url") { "$mockUrl/api/v1" }
            registry.add("PDL.url") { "$mockUrl/graphql" }
            registry.add("REST_STS.url") { "$mockUrl/rest/v1/sts" }
            registry.add("SAF.url") { mockUrl }
            registry.add("SakAPI_v1.url") { "$mockUrl/api/v1/saker" }

            // SOAP services
            registry.add("Dokumentproduksjon_v3.url") { "$mockUrl/soap/services/dokumentproduksjon/v3" }
            registry.add("SakOgBehandling_v1.url") { "$mockUrl/soap/services/sakOgBehandling/v1" }

            // Other integrations
            registry.add("arbeidsforhold.rest.url") { "$mockUrl/aareg-services/api/v1/arbeidstaker/arbeidsforhold" }
            registry.add("ereg.rest.url") { "$mockUrl/ereg/v2" }
            registry.add("inntekt.rest.url") { "$mockUrl/inntektskomponenten/rs/api/v1" }
            registry.add("medlemskap.rest.url") { mockUrl }
            registry.add("microsoft.graph.rest.url") { "$mockUrl/graph/v1.0" }
            registry.add("tilgangsmaskinen.url") { "$mockUrl/tilgangsmaskinen" }
            registry.add("utbetaling_rest.url") { "$mockUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern" }

            log.info("Configured all external service URLs to use mock container at: $mockUrl")
        }
    }
}
