package no.nav.melosys.itest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.types.shouldBeInstanceOf
import mu.KotlinLogging
import no.nav.melosys.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
 * Baseklasse for integrasjonstester som bruker melosys-mock som Docker-container
 * i stedet for in-process mock.
 *
 * Denne klassen kombinerer:
 * - OracleTestContainerBase for database
 * - Melosys-mock container for ekstern tjeneste-mocking
 * - All Spring Boot test-konfigurasjon
 *
 * Nøkkelfunksjoner:
 * - Eksterne tjeneste-URLer peker mot containeren (dynamisk port)
 * - Mock-tilstand tømmes via HTTP-endepunkter, ikke direkte repo-tilgang
 * - Ingen in-process mock-kode brukes
 * - KodeverkTestConfig tilbyr stub kodeverk-data
 *
 * Bruk:
 * 1. Extend denne klassen i stedet for OracleTestContainerBase
 * 2. Bruk mockVerificationClient for å verifisere hva som ble sendt til mockene
 * 3. Alle eksterne tjenestekall går til Docker-containeren
 */
private val log = KotlinLogging.logger { }

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
@Import(KafkaTestConfig::class, KodeverkTestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
class ComponentTestBase : OracleTestContainerBase() {

    @Autowired
    private lateinit var unleash: Unleash

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    val fakeUnleash: FakeUnleash by lazy {
        unleash.shouldBeInstanceOf<FakeUnleash>()
    }

    /**
     * Klient for verifisering av mock-tilstand via HTTP-endepunkter.
     * Peker mot mock-containeren.
     */
    protected val mockVerificationClient: MockVerificationClient by lazy {
        MockVerificationClient(getMockBaseUrl())
    }

    @BeforeEach
    fun componentTestBaseBeforeEach() {
        // Tøm mock-tilstand før hver test
        mockVerificationClient.clear()
    }

    @AfterEach
    fun componentTestBaseAfterEach() {
        // Tøm mock-tilstand via HTTP (container har ikke direkte repo-tilgang)
        mockVerificationClient.clear()
        fakeUnleash.enableAll()
    }

    val Any.toJsonNode: JsonNode
        get() = objectMapper.valueToTree(this)

    companion object {
        /**
         * Docker-image fra Google Artifact Registry.
         * For lokal utvikling, bygg med samme tag: make release (i melosys-docker-compose/mock)
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Intern port brukt av melosys-mock (server.port=8083 i containeren).
         */
        private const val MOCK_PORT = 8083

        private fun useTestContainer(): Boolean = true // easy way to switch to run against local docker

        /**
         * Mock-container-instansen. Deles mellom alle tester som bruker denne baseklassen.
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
         * Henter base-URL for mock-containeren.
         */
        fun getMockBaseUrl(): String {
            return if (useTestContainer()) {
                if (!mockContainer.isRunning) {
                    throw IllegalStateException("Mock container is not running")
                }
                "http://${mockContainer.host}:${mockContainer.getMappedPort(MOCK_PORT)}"
            } else {
                "http://localhost:8083" // bruk mock i docker-compose lokalt
            }
        }

        /**
         * Konfigurerer alle eksterne tjeneste-URLer til å peke mot mock-containeren.
         * Dette overstyrer localhost:8093 URLene i application-test.yml.
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureMockAndDbProperties(registry: DynamicPropertyRegistry) {
            // Start mock-containeren hvis den ikke allerede kjører
            if (!mockContainer.isRunning && useTestContainer()) {
                log.info("Starter melosys-mock container...")
                mockContainer.start()
                log.info("Mock-container startet på: ${getMockBaseUrl()}")
            }

            val mockUrl = getMockBaseUrl()

            // Konfigurer alle eksterne tjeneste-URLer til å peke mot mock-containeren
            // Disse matcher properties i application-test.yml

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

            log.info("Konfigurerte alle eksterne tjeneste-URLer til å bruke mock-container på: $mockUrl")
        }
    }
}
