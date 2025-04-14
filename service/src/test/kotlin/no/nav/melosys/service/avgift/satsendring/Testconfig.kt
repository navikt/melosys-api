package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
class TestConfig {
    @Bean
    fun satsendringAdminController(
        satsendringFinner: SatsendringFinner,
        behandlingService: BehandlingService,
        prosessinstansService: ProsessinstansService
    ) =
        SatsendringAdminController(satsendringFinner, behandlingService, prosessinstansService)

}
