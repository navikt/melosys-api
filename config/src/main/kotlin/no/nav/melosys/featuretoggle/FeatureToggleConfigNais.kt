package no.nav.melosys.featuretoggle

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger { }

@Configuration
@Profile("nais")
class FeatureToggleConfigNais {

    @Bean
    fun unleash(@Value("\${unleash.token}") token: String,
                @Value("\${unleash.url}") url: String): Unleash {
        val unleashConfig = UnleashConfig.builder()
            .apiKey(token)
            .appName(APP_NAME)
            .unleashAPI(url)
            .build()

        // Fra v10+ er GradualRollout- og UserWithId-strategiene innebygd i SDK-en.
        // Vi trenger kun å registrere vår egen custom strategi (ByUserIdStrategy).
        return DefaultUnleash(
            unleashConfig,
            ByUserIdStrategy()
        ).also { log.info { "FeatureToggleConfigNais er aktivert med ${it.javaClass.simpleName}" } }
    }

    companion object {
        const val APP_NAME = "Melosys-api"
    }
}
