package no.nav.melosys.itest

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.utility.DockerImageName

open class OracleTestContainerBase {
    companion object {
        var oracleContainer = OracleContainer(
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

        private fun useTestContainer(): Boolean =
            System.getenv("USE-LOCAL-DB")?.lowercase() != "true" && useContainer
    }
}
