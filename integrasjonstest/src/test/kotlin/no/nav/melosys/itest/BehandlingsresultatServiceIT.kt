package no.nav.melosys.itest

import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles(profiles = ["test"])
@ExtendWith(SpringExtension::class)
@DataJpaTest(
    excludeAutoConfiguration = [FlywayAutoConfiguration::class],
    properties = ["spring.profiles.active:test"],

    )
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories("no.nav.melosys.repository")
@EntityScan("no.nav.melosys.domain")
@Import(
    value = [
        BehandlingsresultatService::class,
    ]
)
internal class BehandlingsresultatServiceIT {

    @Autowired
    private val behandlingsresultatService: BehandlingsresultatService? = null

    @Test
    fun lest_ut_data_fra_orale_db() {
        val hentBehandlingsresultat = behandlingsresultatService!!.hentBehandlingsresultat(1L)
        println(hentBehandlingsresultat)
    }
}
