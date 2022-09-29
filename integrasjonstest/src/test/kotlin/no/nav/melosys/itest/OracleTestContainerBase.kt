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

        @DynamicPropertySource
        @JvmStatic
        fun oracleProperties(registry: DynamicPropertyRegistry) {
            // Det finnes ikke Oracle container som støtter ARM arkitektur
            val m1Mac: String? = System.getenv("M1-MAC")
            if (m1Mac?.lowercase() == "true") return

            registry.add("spring.datasource.url") { oracleContainer.jdbcUrl }
            registry.add("spring.datasource.password") { oracleContainer.password }
            registry.add("spring.datasource.username") { oracleContainer.username }

            oracleContainer.start()
        }
    }
}
