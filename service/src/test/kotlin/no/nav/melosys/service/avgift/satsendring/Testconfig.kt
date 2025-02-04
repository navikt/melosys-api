package no.nav.melosys.service.avgift.satsendring

import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
class TestConfig {
    @Bean
    fun satsendringAdminController(satsendringFinner: SatsendringFinner): SatsendringAdminController {
        return SatsendringAdminController(satsendringFinner, "Dummy")
    }
}
