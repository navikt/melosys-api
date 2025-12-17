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
 * Baseklasse for integrasjonstester som bruker melosys-mock som Docker-container
 * i stedet for in-process mock.
 *
 * Denne klassen:
 * - Starter melosys-mock containeren ved hjelp av Testcontainers
 * - Konfigurerer alle eksterne tjeneste-URLer til å peke mot containeren
 * - Tilbyr en mockVerificationClient for å verifisere mock-tilstand
 *
 * Bruk:
 * 1. Extend denne klassen i stedet for OracleTestContainerBase
 * 2. Bruk mockVerificationClient for å verifisere hva som ble sendt til mockene
 * 3. Containeren kjører på en dynamisk port, alle URLer konfigureres automatisk
 *
 * Merk: Tester som bruker denne baseklassen vil IKKE ha in-process mock-kode aktiv.
 * Alle eksterne tjenestekall går til Docker-containeren.
 */
open class MelosysMockContainerTestBase {

    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainerTestBase::class.java)

        /**
         * Docker-image fra Google Artifact Registry.
         * Kan også bruke lokalt image: melosys-docker-compose-mock:latest
         * Bygg lokalt med: cd melosys-docker-compose && make build-mock
         */
        private const val MOCK_IMAGE = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Intern port brukt av melosys-mock (server.port=8083 i containeren).
         */
        private const val MOCK_PORT = 8083

        /**
         * Om mock-containeren skal brukes. Sett til false for å bruke in-process mock.
         */
        private const val USE_CONTAINER = true

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
            // Kafka bootstrap servers ikke nødvendig for mock-only tester
            .withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")

        /**
         * Henter base-URL for mock-containeren.
         */
        fun getMockBaseUrl(): String {
            if (!mockContainer.isRunning) {
                throw IllegalStateException("Mock container is not running")
            }
            return "http://${mockContainer.host}:${mockContainer.getMappedPort(MOCK_PORT)}"
        }

        /**
         * Konfigurerer alle eksterne tjeneste-URLer til å peke mot mock-containeren.
         * Dette overstyrer localhost:8093 URLene i application-test.yml.
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureMockProperties(registry: DynamicPropertyRegistry) {
            if (!USE_CONTAINER) {
                log.info("Mock-container deaktivert, bruker in-process mock")
                return
            }

            // Start containeren hvis den ikke allerede kjører
            if (!mockContainer.isRunning) {
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
            registry.add("JournalpostApi_v1.url") { "$mockUrl/rest/journalpostapi/v1" }
            registry.add("KodeverkAPI_v1.url") { "$mockUrl/api" }
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

    /**
     * Klient for verifisering av mock-tilstand via HTTP-endepunkter.
     * Peker mot containerens verifikasjonsendepunkter.
     */
    protected val mockVerificationClient: MockVerificationClient by lazy {
        if (USE_CONTAINER && mockContainer.isRunning) {
            MockVerificationClient(getMockBaseUrl())
        } else {
            // Fallback til in-process mock
            MockVerificationClient("http://localhost:8093")
        }
    }
}
