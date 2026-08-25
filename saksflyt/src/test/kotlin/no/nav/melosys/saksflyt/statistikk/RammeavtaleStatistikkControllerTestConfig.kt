package no.nav.melosys.saksflyt.statistikk

import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean

/** Minimal kontekst for [RammeavtaleStatistikkControllerTest] — `@WebMvcTest` trenger en `@SpringBootConfiguration`. */
@SpringBootConfiguration
class RammeavtaleStatistikkControllerTestConfig {

    @Bean
    fun rammeavtaleStatistikkController(rammeavtaleStatistikkService: RammeavtaleStatistikkService) =
        RammeavtaleStatistikkController(rammeavtaleStatistikkService)
}
