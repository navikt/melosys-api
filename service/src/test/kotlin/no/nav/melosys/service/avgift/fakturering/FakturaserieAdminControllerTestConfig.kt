package no.nav.melosys.service.avgift.fakturering

import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
class FakturaserieAdminControllerTestConfig {

    @Bean
    fun fakturaserieAdminController(behandlingsresultatService: BehandlingsresultatService) =
        FakturaserieAdminController(behandlingsresultatService)
}
