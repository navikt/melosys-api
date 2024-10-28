package no.nav.melosys.itest

import no.nav.melosys.DBCleanup
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.DockerImageName


open class OracleTestContainerBase {
    private val dbCleanUpActions = mutableListOf<() -> Unit>()

    @Autowired
    var dbCleanup: DBCleanup? = null

    protected fun addCleanUpAction(deleteAction: DBCleanup.() -> Unit) {
        dbCleanUpActions.add { dbCleanup?.deleteAction() }
    }

    @AfterEach
    fun oracleTestContainerBaseAfterEach() {
        dbCleanUpActions.forEach { it() }
    }

    companion object {
        val oracleContainer = OracleContainer(
            DockerImageName.parse("ghcr.io/navikt/melosys-legacy-avhengigheter/oracle-xe:18.4.0-slim")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe")
        )
        private const val useContainer = false // easy way to switch to run against local docker

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
