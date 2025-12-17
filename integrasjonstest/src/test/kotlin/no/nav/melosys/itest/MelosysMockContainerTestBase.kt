package no.nav.melosys.itest

import org.slf4j.LoggerFactory
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
 * This class:
 * - Starts the melosys-mock container using Testcontainers
 * - Configures all external service URLs to point to the container
 * - Provides a mockVerificationClient for verifying mock state
 *
 * Usage:
 * 1. Extend this class instead of OracleTestContainerBase
 * 2. Use mockVerificationClient to verify what was sent to mocks
 * 3. The container runs on a dynamic port, all URLs are configured automatically
 *
 * Note: Tests using this base class will NOT have the in-process mock code active.
 * All external service calls go to the Docker container.
 */
open class MelosysMockContainerTestBase {

    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainerTestBase::class.java)

        /**
         * Docker image from Google Artifact Registry.
         * Can also use local image: melosys-docker-compose-mock:latest
         * Build locally with: cd melosys-docker-compose && make build-mock
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Internal port used by melosys-mock (server.port=8083 in the container).
         */
        private const val MOCK_PORT = 8083

        /**
         * Whether to use the mock container. Set to false to use in-process mock.
         */
        private const val USE_CONTAINER = true

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
            // Kafka bootstrap servers not needed for mock-only tests
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
        fun configureMockProperties(registry: DynamicPropertyRegistry) {
            if (!USE_CONTAINER) {
                log.info("Mock container disabled, using in-process mock")
                return
            }

            // Start the container if not already running
            if (!mockContainer.isRunning) {
                log.info("Starting melosys-mock container...")
                mockContainer.start()
                log.info("Mock container started at: ${getMockBaseUrl()}")
            }

            val mockUrl = getMockBaseUrl()

            // Configure all external service URLs to point to the mock container
            // These match the properties in application-test.yml

            // REST APIs
            registry.add("DistribuerJournalpost_v1.url") { "$mockUrl/rest/v1/distribuerjournalpost" }
            registry.add("Inngangsvilkaar.url") { "$mockUrl/api" }
            registry.add("JournalpostApi_v1.url") { "$mockUrl/rest/journalpostapi/v1" }
            registry.add("KodeverkAPI_v1.url") { "$mockUrl/api" }
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

    /**
     * Client for verifying mock state via HTTP endpoints.
     * Points to the container's verification endpoints.
     */
    protected val mockVerificationClient: MockVerificationClient by lazy {
        if (USE_CONTAINER && mockContainer.isRunning) {
            MockVerificationClient(getMockBaseUrl())
        } else {
            // Fallback to in-process mock
            MockVerificationClient("http://localhost:8093")
        }
    }
}
