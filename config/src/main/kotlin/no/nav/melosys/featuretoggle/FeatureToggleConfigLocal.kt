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
@Profile("!nais & !test")
class FeatureToggleConfigLocal {

    @Value("\${unleash.url:}")
    private lateinit var unleashUrl: String

    @Value("\${unleash.token:}")
    private lateinit var unleashToken: String

    @Value("\${unleash.app-name:melosys-api-local}")
    private lateinit var unleashAppName: String

    @Value("\${unleash.environment:development}")
    private lateinit var unleashEnvironment: String

    @Bean
    fun unleash(): Unleash {
        // Hvis Unleash URL er konfigurert, bruk ekte Unleash-server med default-enabled wrapper
        return if (unleashUrl.isNotBlank() && unleashToken.isNotBlank()) {
            val config = UnleashConfig.builder()
                .appName(unleashAppName)
                .instanceId("$unleashAppName-instance")
                .unleashAPI(unleashUrl)
                .apiKey(unleashToken)
                .environment(unleashEnvironment)
                .build()

            val defaultUnleash = DefaultUnleash(config)

            // Wrapper med DefaultEnabledUnleash for å defaulte ukjente toggles til enabled
            DefaultEnabledUnleash(defaultUnleash).also {
                log.info { "FeatureToggleConfigLocal: Bruker DefaultEnabledUnleash som wrapper for Unleash-server på $unleashUrl" }
            }
        } else {
            // Fallback til LocalUnleash hvis Unleash-server ikke er konfigurert
            LocalUnleash().apply {
                enableAllExcept(ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT)
            }.also {
                log.info { "FeatureToggleConfigLocal: Bruker LocalUnleash (ingen Unleash-server konfigurert)" }
            }
        }
    }
}
