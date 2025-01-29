package no.nav.melosys.featuretoggle

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.GradualRolloutSessionIdStrategy
import io.getunleash.strategy.GradualRolloutUserIdStrategy
import io.getunleash.strategy.UserWithIdStrategy
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
    fun unleash(@Value("\${unleash.token}") token: String): Unleash {
        val unleashConfig = UnleashConfig.builder()
            .apiKey(token)
            .appName(APP_NAME)
            .unleashAPI(UNLEASH_URL)
            .build()

        return DefaultUnleash(
            unleashConfig,
            GradualRolloutSessionIdStrategy(),
            GradualRolloutUserIdStrategy(),
            GradualRolloutRandomStrategy(),
            UserWithIdStrategy(),
            ByUserIdStrategy()
        ).also { log.info { "FeatureToggleConfigNais er aktivert med ${it.javaClass.simpleName}" } }
    }

    companion object {
        const val UNLEASH_URL = "https://melosys-unleash-api.nav.cloud.nais.io/api"
        const val APP_NAME = "Melosys-api"
    }
}
