package no.nav.melosys.itest

import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Konfigurasjon for melosys-mock Docker-container.
 *
 * Dette objektet håndterer:
 * - Opprettelse og livssyklusadministrasjon av mock-containeren
 * - Konfigurasjon av Spring-properties for eksterne tjeneste-URLer
 * - Deling av container-instans mellom alle tester
 *
 * Bruk:
 * - I @DynamicPropertySource: MelosysMockContainerConfig.configureProperties(registry)
 * - For base-URL: MelosysMockContainerConfig.getBaseUrl()
 */
object MelosysMockContainerConfig {
    private val log = KotlinLogging.logger { }

    /**
     * Docker-image fra Google Artifact Registry.
     * For lokal utvikling, bygg med samme tag: make release (i melosys-docker-compose/mock)
     */
    const val IMAGE_NAME = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

    /**
     * Intern port brukt av melosys-mock (server.port=8083 i containeren).
     */
    private const val MOCK_PORT = 8083

    /**
     * Om testcontainer skal brukes. Sett til false for å kjøre mot lokal docker-compose.
     * Kan overstyres med miljøvariabelen USE_TEST_CONTAINER=false.
     */
     private val USE_TEST_CONTAINER: Boolean = System.getenv("USE_TEST_CONTAINER")?.toBoolean() ?: true

    /**
     * Logger for container-output.
     */
    private val mockLog = LoggerFactory.getLogger("melosys-mock")

    /**
     * Mock-container-instansen. Deles mellom alle tester.
     */
    val container: GenericContainer<*> = GenericContainer(DockerImageName.parse(IMAGE_NAME))
        .withExposedPorts(MOCK_PORT)
        .withLogConsumer(Slf4jLogConsumer(mockLog))
        .waitingFor(
            Wait.forHttp("/actuator/health")
                .forPort(MOCK_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(2))
        )
        .withEnv("SPRING_PROFILES_ACTIVE", "test")
        // Kafka brukes ikke av mock-containeren, men Spring Boot krever at property er satt.
        // Verdien ignoreres siden mocken ikke kobler til Kafka.
        .withEnv("KAFKA_BOOTSTRAP_SERVERS", "placeholder:9092")

    /**
     * Starter containeren hvis den ikke allerede kjører.
     */
    fun startIfNeeded() {
        if (USE_TEST_CONTAINER && !container.isRunning) {
            log.info { "Starter melosys-mock container..." }
            container.start()
            log.info { "Mock-container startet på: ${getBaseUrl()}" }
        }
    }

    /**
     * Henter base-URL for mock-containeren.
     */
    fun getBaseUrl(): String {
        return if (USE_TEST_CONTAINER) {
            if (!container.isRunning) {
                throw IllegalStateException("Mock container is not running")
            }
            "http://${container.host}:${container.getMappedPort(MOCK_PORT)}"
        } else {
            "http://localhost:8083" // bruk mock i docker-compose lokalt
        }
    }

    /**
     * Konfigurerer alle eksterne tjeneste-URLer til å peke mot mock-containeren.
     * Kall denne fra @DynamicPropertySource-metoden i testklassen.
     */
    fun configureProperties(registry: DynamicPropertyRegistry) {
        startIfNeeded()
        val mockUrl = getBaseUrl()

        // REST APIer
        registry.add("DistribuerJournalpost_v1.url") { "$mockUrl/rest/v1/distribuerjournalpost" }
        registry.add("Inngangsvilkaar.url") { "$mockUrl/api" }
        registry.add("KodeverkAPI_v1.url") { "$mockUrl/api" }
        registry.add("JournalpostApi_v1.url") { "$mockUrl/rest/journalpostapi/v1" }
        registry.add("MelosysEessi.url") { "$mockUrl/api" }
        registry.add("OppgaveAPI_v1.url") { "$mockUrl/api/v1" }
        registry.add("PDL.url") { "$mockUrl/graphql" }
        registry.add("REST_STS.url") { "$mockUrl/rest/v1/sts" }
        registry.add("SAF.url") { mockUrl }
        registry.add("SakAPI_v1.url") { "$mockUrl/api/v1/saker" }

        // SOAP-tjenester
        registry.add("Dokumentproduksjon_v3.url") { "$mockUrl/soap/services/dokumentproduksjon/v3" }
        registry.add("SakOgBehandling_v1.url") { "$mockUrl/soap/services/sakOgBehandling/v1" }

        // Andre integrasjoner
        registry.add("arbeidsforhold.rest.url") { "$mockUrl/aareg-services/api/v1/arbeidstaker/arbeidsforhold" }
        registry.add("ereg.rest.url") { "$mockUrl/ereg/v2" }
        registry.add("inntekt.rest.url") { "$mockUrl/inntektskomponenten/rs/api/v1" }
        registry.add("medlemskap.rest.url") { mockUrl }
        registry.add("microsoft.graph.rest.url") { "$mockUrl/graph/v1.0" }
        registry.add("tilgangsmaskinen.url") { "$mockUrl/tilgangsmaskinen" }
        registry.add("utbetaling_rest.url") { "$mockUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern" }

        log.info { "Konfigurerte alle eksterne tjeneste-URLer til å bruke mock-container på: $mockUrl" }
    }
}
