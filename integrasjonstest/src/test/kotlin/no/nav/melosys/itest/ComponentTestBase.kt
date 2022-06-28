package no.nav.melosys.itest

import no.nav.melosys.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    properties = ["spring.profiles.active:test"],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@Import(
    ComponentTestConfig::class
)
@EnableMockOAuth2Server
class ComponentTestBase {
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
