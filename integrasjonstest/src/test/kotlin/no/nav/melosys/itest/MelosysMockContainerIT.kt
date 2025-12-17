package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Integration test demonstrating melosys-mock container capabilities.
 *
 * This test verifies that:
 * 1. The melosys-mock container starts successfully
 * 2. The verification endpoints are accessible
 * 3. The mock responds to API calls
 *
 * NOTE: This is a standalone test that only tests the container itself,
 * not the full melosys-api integration. For full integration tests with
 * the container, see MelosysMockContainerTestBase.
 */
class MelosysMockContainerIT {

    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainerIT::class.java)

        /**
         * Mock image from Google Artifact Registry.
         * For local development, you can also use the local image built from melosys-docker-compose.
         * Local: melosys-docker-compose-mock:latest
         * GAR: europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"
        private const val MOCK_PORT = 8083
    }

    /**
     * Create a GenericContainer for melosys-mock.
     */
    private fun createMockContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse(MOCK_IMAGE))
            .withExposedPorts(MOCK_PORT)
            .withLogConsumer(Slf4jLogConsumer(log).withPrefix("melosys-mock"))
            .waitingFor(
                Wait.forHttp("/actuator/health")
                    .forPort(MOCK_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2))
            )
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
    }

    @Test
    fun `should start mock container and access verification endpoints`() {
        // Create and start the container (using local image)
        val container = createMockContainer()

        container.use { mock ->
            mock.start()
            val baseUrl = "http://${mock.host}:${mock.getMappedPort(MOCK_PORT)}"
            log.info("Mock container started at: $baseUrl")

            // Create verification client pointing to container
            val client = MockVerificationClient(baseUrl)

            // Verify health check
            client.isHealthy() shouldBe true

            // Verify summary endpoint works (should return zeros for empty mock)
            val summary = client.summary()
            summary shouldNotBe null
            summary.medlCount shouldBe 0
            summary.sakCount shouldBe 0
            summary.oppgaveCount shouldBe 0

            // Verify individual verification endpoints work
            client.medl() shouldBe emptyList()
            client.saker() shouldBe emptyList()
            client.oppgaver() shouldBe emptyList()
            client.journalposter() shouldBe emptyList()

            log.info("All verification endpoints accessible and working correctly")
        }
    }

    @Test
    fun `should verify clear endpoint works`() {
        val container = createMockContainer()

        container.use { mock ->
            mock.start()
            val baseUrl = "http://${mock.host}:${mock.getMappedPort(MOCK_PORT)}"
            val client = MockVerificationClient(baseUrl)

            // Clear should work even on empty mock
            val clearResponse = client.clear()
            clearResponse.message shouldNotBe null
            log.info("Clear response: ${clearResponse.message}")
        }
    }

    @Test
    @Disabled("Manual test - run to inspect container startup time")
    fun `manual - measure container startup time`() {
        val startTime = System.currentTimeMillis()
        val container = createMockContainer()

        container.use { mock ->
            mock.start()
            val startupTime = System.currentTimeMillis() - startTime
            log.info("Container startup time: ${startupTime}ms (${startupTime / 1000}s)")

            val client = MockVerificationClient("http://${mock.host}:${mock.getMappedPort(MOCK_PORT)}")
            client.isHealthy() shouldBe true
        }
    }
}
