package no.nav.melosys.itest

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles(profiles = ["test"])
@ExtendWith(SpringExtension::class)
@DataJpaTest(
    showSql = false,
    properties = ["spring.profiles.active:test"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories("no.nav.melosys.repository")
@EntityScan("no.nav.melosys.domain")
class DataJpaTestBase() : OracleTestContainerBase() {
}
