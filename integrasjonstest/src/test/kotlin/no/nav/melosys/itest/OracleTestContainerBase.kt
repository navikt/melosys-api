package no.nav.melosys.itest

import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.DockerImageName

private val log = KotlinLogging.logger { }

open class OracleTestContainerBase {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /**
     * Tømmer alle tabeller før hver test for å sikre isolasjon mellom tester.
     *
     * Fordeler med BeforeEach fremfor AfterEach:
     * - Hver test starter med en ren database
     * - Man kan inspisere databasen etter en test for feilsøking
     *
     * Ekskluderer Flyway-tabeller og kodeverk/statiske data-tabeller.
     */
    @BeforeEach
    fun truncateAllTables() {
        val tablesToTruncate = jdbcTemplate.queryForList(
            """
            SELECT table_name FROM user_tables
            WHERE table_name NOT LIKE 'flyway%'
              AND table_name NOT IN (
                'KODEVERK', 'KODEVERK_RELASJON', 'KODEVERK_RELASJON_VERDI',
                'BEHANDLINGSAARSAK_TYPE', 'BEHANDLINGSTYPE', 'BEHANDLINGSTEMA',
                'BEHANDLINGSRESULTATTYPE', 'FAGSAK_STATUS', 'FAGSAK_TYPE',
                'LOVVALGSBESTEMMELSE', 'PROSESS_STEG', 'PROSESS_TYPE',
                'SAKSTYPE', 'UTENLANDSK_MYNDIGHET', 'PREFERANSE'
              )
            """,
            String::class.java
        )

        if (tablesToTruncate.isEmpty()) {
            return
        }

        // Deaktiver alle fremmednøkkel-constraints
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT constraint_name, table_name FROM user_constraints
            WHERE constraint_type = 'R' AND status = 'ENABLED'
            """,
            mapOf<String, Any>()
        )

        constraints.forEach { row ->
            val constraintName = row["CONSTRAINT_NAME"] as String
            val tableName = row["TABLE_NAME"] as String
            try {
                jdbcTemplate.execute("ALTER TABLE $tableName DISABLE CONSTRAINT $constraintName")
            } catch (e: Exception) {
                log.warn { "Kunne ikke deaktivere constraint $constraintName på $tableName: ${e.message}" }
            }
        }

        // Tøm alle tabeller
        tablesToTruncate.forEach { tableName ->
            try {
                jdbcTemplate.execute("TRUNCATE TABLE $tableName")
            } catch (e: Exception) {
                log.warn { "Kunne ikke tømme tabell $tableName: ${e.message}" }
            }
        }

        // Reaktiver alle fremmednøkkel-constraints
        constraints.forEach { row ->
            val constraintName = row["CONSTRAINT_NAME"] as String
            val tableName = row["TABLE_NAME"] as String
            try {
                jdbcTemplate.execute("ALTER TABLE $tableName ENABLE CONSTRAINT $constraintName")
            } catch (e: Exception) {
                log.warn { "Kunne ikke reaktivere constraint $constraintName på $tableName: ${e.message}" }
            }
        }
    }

    companion object {
        val oracleContainer = OracleContainer(
            DockerImageName.parse("ghcr.io/navikt/melosys-legacy-avhengigheter/oracle-xe:18.4.0-slim")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe")
        )
        private const val useContainer = true // easy way to switch to run against local docker

        @DynamicPropertySource
        @JvmStatic
        fun oracleProperties(registry: DynamicPropertyRegistry) {
            if (useTestContainer()) {
                registry.add("spring.datasource.url") { oracleContainer.jdbcUrl }
                registry.add("spring.datasource.password") { oracleContainer.password }
                registry.add("spring.datasource.username") { oracleContainer.username }

                oracleContainer.start()
            }
        }

        private fun useLocalDB(): Boolean = listOf("USE-LOCAL-DB", "USE_LOCAL_DB").any { System.getenv(it)?.lowercase() == "true" }

        private fun useTestContainer(): Boolean = !useLocalDB() && useContainer
    }
}
