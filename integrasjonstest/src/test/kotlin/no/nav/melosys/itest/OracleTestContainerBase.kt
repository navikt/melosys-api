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
     * Bruker eksplisitt liste over tabeller som skal tømmes for å unngå
     * å tømme kodeverk/statiske data-tabeller.
     */
    @BeforeEach
    fun truncateAllTables() {
        // Eksplisitt liste over tabeller som inneholder testdata (ikke kodeverk/statiske data)
        val tablesToTruncate = listOf(
            "PROSESSINSTANS_HENDELSER", "PROSESSINSTANS",  // Saksflyt først pga FK
            "AARSAVREGNING", "TRYGDEAVGIFTSPERIODE_GRUNNLAG", "TRYGDEAVGIFTSPERIODE", "SKATTEFORHOLD_TIL_NORGE", "INNTEKTSPERIODE",
            "HELSEUTGIFT_DEKKES_PERIODE", "MEDLEMSKAPSPERIODE", "LOVVALG_PERIODE", "UTPEKINGSPERIODE",
            "VILKAARSRESULTAT", "VILKAAR_BEGRUNNELSE", "AVKLARTEFAKTA", "AVKLARTEFAKTA_REGISTRERING",
            "VEDTAK_METADATA", "BEHANDLINGSRES_BEGRUNNELSE", "KONTROLLRESULTAT", "UTKAST_BREV",
            "SAKSOPPLYSNING_KILDE", "SAKSOPPLYSNING", "MOTTATTEOPPLYSNINGER",
            "ANMODNINGSPERIODE_SVAR", "ANMODNINGSPERIODE",
            "TIDLIGERE_MEDLEMSPERIODE", "KONTAKTOPPLYSNING", "FULLMAKT",
            "BEHANDLINGSNOTAT", "BEHANDLINGSAARSAK", "OPPGAVE_TILBAKELEGGING",
            "BEHANDLINGSRESULTAT", "BEHANDLING", "AKTOER", "FAGSAK"
        )

        // Deaktiver alle fremmednøkkel-constraints
        data class Constraint(val name: String, val tableName: String)
        val constraints = jdbcTemplate.query(
            "SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R' AND status = 'ENABLED'"
        ) { rs, _ -> Constraint(rs.getString("CONSTRAINT_NAME"), rs.getString("TABLE_NAME")) }

        constraints.forEach { constraint ->
            try {
                jdbcTemplate.execute("ALTER TABLE ${constraint.tableName} DISABLE CONSTRAINT ${constraint.name}")
            } catch (e: Exception) {
                log.warn { "Kunne ikke deaktivere constraint ${constraint.name} på ${constraint.tableName}: ${e.message}" }
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
        constraints.forEach { constraint ->
            try {
                jdbcTemplate.execute("ALTER TABLE ${constraint.tableName} ENABLE CONSTRAINT ${constraint.name}")
            } catch (e: Exception) {
                log.warn { "Kunne ikke reaktivere constraint ${constraint.name} på ${constraint.tableName}: ${e.message}" }
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
