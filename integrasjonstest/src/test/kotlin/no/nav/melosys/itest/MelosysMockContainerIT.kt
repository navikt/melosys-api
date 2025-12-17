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
 * Integrasjonstest som demonstrerer melosys-mock container-funksjonalitet.
 *
 * Denne testen verifiserer at:
 * 1. Melosys-mock containeren starter vellykket
 * 2. Verifikasjonsendepunktene er tilgjengelige
 * 3. Mocken svarer på API-kall
 *
 * MERK: Dette er en frittstående test som kun tester selve containeren,
 * ikke full melosys-api integrasjon. For fulle integrasjonstester med
 * containeren, se MelosysMockContainerTestBase.
 */
class MelosysMockContainerIT {

    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainerIT::class.java)

        /**
         * Mock-image fra Google Artifact Registry.
         * For lokal utvikling kan du også bruke det lokale imaget bygget fra melosys-docker-compose.
         * Lokalt: melosys-docker-compose-mock:latest
         * GAR: europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"
        private const val MOCK_PORT = 8083
    }

    /**
     * Oppretter en GenericContainer for melosys-mock.
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
        // Opprett og start containeren (bruker lokalt image)
        val container = createMockContainer()

        container.use { mock ->
            mock.start()
            val baseUrl = "http://${mock.host}:${mock.getMappedPort(MOCK_PORT)}"
            log.info("Mock container started at: $baseUrl")

            // Opprett verifikasjonsklient som peker mot containeren
            val client = MockVerificationClient(baseUrl)

            // Verifiser helsesjekk
            client.isHealthy() shouldBe true

            // Verifiser at oppsummerings-endepunktet fungerer (bør returnere nuller for tom mock)
            val summary = client.summary()
            summary shouldNotBe null
            summary.medlCount shouldBe 0
            summary.sakCount shouldBe 0
            summary.oppgaveCount shouldBe 0

            // Verifiser at individuelle verifikasjonsendepunkter fungerer
            client.medl() shouldBe emptyList()
            client.saker() shouldBe emptyList()
            client.oppgaver() shouldBe emptyList()
            client.journalposter() shouldBe emptyList()

            log.info("Alle verifikasjonsendepunkter tilgjengelige og fungerer korrekt")
        }
    }

    @Test
    fun `should verify clear endpoint works`() {
        val container = createMockContainer()

        container.use { mock ->
            mock.start()
            val baseUrl = "http://${mock.host}:${mock.getMappedPort(MOCK_PORT)}"
            val client = MockVerificationClient(baseUrl)

            // Tømming bør fungere selv på tom mock
            val clearResponse = client.clear()
            clearResponse.message shouldNotBe null
            log.info("Clear response: ${clearResponse.message}")
        }
    }

    @Test
    @Disabled("Manuell test - kjør for å inspisere container-oppstartstid")
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
