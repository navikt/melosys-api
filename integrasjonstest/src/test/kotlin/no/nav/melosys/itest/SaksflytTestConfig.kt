package no.nav.melosys.itest

import no.nav.melosys.melosysmock.config.GraphqlConfig
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableJpaRepositories("no.nav.melosys.repository")
@AutoConfigureDataJpa
@EntityScan("no.nav.melosys.domain")
@Import(GraphqlConfig::class)
class SaksflytTestConfig {
    init {
        System.setProperty("kafkaPort", "0")
    }
}
