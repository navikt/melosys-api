package no.nav.melosys.itest

import no.nav.melosys.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@Import(
    ComponentTestConfig::class
)
@DirtiesContext
@EnableMockOAuth2Server
class ComponentTestBase : OracleTestContainerBase()
