package no.nav.melosys.itest

import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Testcontainer for melosys-mock Docker-image.
 *
 * Denne containeren kjører samme mock som brukes for lokal utvikling og E2E-tester,
 * noe som eliminerer behovet for duplisert in-process mock-kode.
 *
 * Mocken tilbyr:
 * - MEDL API mock
 * - SAF/Joark API mock
 * - Oppgave API mock
 * - Melosys-eessi API mock
 * - PDL GraphQL mock
 * - Og mer...
 *
 * Verifikasjonsendepunkter er tilgjengelige på /testdata/verification/ for å
 * verifisere hvilke data som ble sendt til mockene under tester.
 */
class MelosysMockContainer : GenericContainer<MelosysMockContainer>(
    DockerImageName.parse(IMAGE_NAME)
) {
    companion object {
        private val log = LoggerFactory.getLogger(MelosysMockContainer::class.java)

        /**
         * Docker-image fra Google Artifact Registry.
         * Kan også bruke lokalt image: melosys-docker-compose-mock:latest
         * Bygg lokalt med: cd melosys-docker-compose && make build-mock
         */
        const val IMAGE_NAME = "europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-docker-compose-mock:latest"

        /**
         * Port eksponert av mock-containeren.
         * Merk: melosys-mock bruker 8083 internt (server.port=8083).
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
        // Sørg for at Kafka bootstrap servers er konfigurert (mock trenger dette selv om Kafka ikke brukes)
        withEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        withEnv("SPRING_PROFILES_ACTIVE", "test")
    }

    /**
     * Henter base-URL for mock-containeren.
     * Bruk denne for å konfigurere integrasjons-URLer i tester.
     */
    fun getBaseUrl(): String = "http://$host:${getMappedPort(MOCK_PORT)}"

    /**
     * Henter URL for en spesifikk sti.
     */
    fun getUrl(path: String): String = "${getBaseUrl()}${if (path.startsWith("/")) path else "/$path"}"
}
