package no.nav.melosys.service.sak

import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
class SkjemaSaksstatusAdminControllerTestConfig {

    @Bean
    fun skjemaSaksstatusAdminController(skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService) =
        SkjemaSaksstatusAdminController(skjemaSaksstatusSyncService)
}
