package no.nav.melosys.itest

import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Testcontainer for melosys-mock Docker image.
 *
 * This container runs the same mock that is used for local development and E2E tests,
 * eliminating the need for duplicate in-process mock code.
 *
 * The mock provides:
 * - MEDL API mock
 * - SAF/Joark API mock
 * - Oppgave API mock
 * - Melosys-eessi API mock
 * - PDL GraphQL mock
 * - And more...
 *
 * Verification endpoints are available at /testdata/verification/ for asserting
 * what data was sent to the mocks during tests.
 */
class MelosysMockContainer : GenericContainer<MelosysMockContainer>(
    DockerImageName.parse(IMAGE_NAME)
) {
    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainer::class.java)

        /**
         * Docker image from Google Artifact Registry.
         * Can also use local image: melosys-docker-compose-mock:latest
         * Build locally with: cd melosys-docker-compose && make build-mock
         */
        const val IMAGE_NAME = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Port exposed by the mock container.
         * Note: melosys-mock uses 8083 internally (server.port=8083).
         */
        const val MOCK_PORT = 8083
    }

    init {
        withExposedPorts(MOCK_PORT)
        withLogConsumer(Slf4jLogConsumer(log).withPrefix("melosys-mock"))
        waitingFor(
            Wait.forHttp("/actuator/health")
                .forPort(MOCK_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(2))
        )
        // Ensure Kafka bootstrap servers are configured (mock needs this even if not using Kafka)
        withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        withEnv("SPRING_PROFILES_ACTIVE", "test")
    }

    /**
     * Get the base URL for the mock container.
     * Use this to configure integration URLs in tests.
     */
    fun getBaseUrl(): String = "http://$host:${getMappedPort(MOCK_PORT)}"

    /**
     * Get URL for a specific path.
     */
    fun getUrl(path: String): String = "${getBaseUrl()}${if (path.startsWith("/")) path else "/$path"}"
}
