package no.nav.melosys.featuretoggle

import io.getunleash.Unleash
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger { }

@Configuration
@Profile("!nais & !test")
class FeatureToggleConfigLocal {

    @Bean
    fun unleash(): Unleash = LocalUnleash().apply { enableAllExcept(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT) }.also {
        log.info { "FeatureToggleConfigLocal er aktivert med ${it.javaClass.simpleName}" }
    }
}
