package no.nav.melosys.itest

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
open class OracleTestConteinerBase {
    companion object {
        @Container
        var DB = OracleContainer()

        @DynamicPropertySource
        @JvmStatic
        fun oracleProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { DB.jdbcUrl }
            registry.add("spring.datasource.password") { DB.password }
            registry.add("spring.datasource.username") { DB.username }
        }
    }
}
