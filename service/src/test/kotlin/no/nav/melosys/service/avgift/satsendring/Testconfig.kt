package no.nav.melosys.service.avgift.satsendring

import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootConfiguration
@Configuration
class TestConfig {
    @Bean
    fun satsendringAdminController(satsendringFinner: SatsendringFinner): SatsendringAdminController {
        return SatsendringAdminController(satsendringFinner, "Dummy")
    }
}
