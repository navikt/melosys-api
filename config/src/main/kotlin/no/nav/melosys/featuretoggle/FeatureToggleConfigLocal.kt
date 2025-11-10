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

    @Value("\${unleash.admin-token:}")
    private lateinit var unleashAdminToken: String

    @Value("\${unleash.project-id:default}")
    private lateinit var unleashProjectId: String

    /**
     * Toggles som skal være disabled ved automatisk opprettelse.
     * Disse kan enablees manuelt i Unleash UI etter behov.
     */
    private val autoDisabledToggles = setOf(
        ToggleName.MELOSYS_ÅRSAVREGNING_UTEN_FLYT,
    )

    @Bean
    fun unleash(): Unleash {
        // Hvis Unleash URL er konfigurert, bruk ekte Unleash-server med default-enabled wrapper
        return if (unleashUrl.isNotBlank() && unleashToken.isNotBlank()) {
            // Synkroniser alle toggles til Unleash før vi oppretter Unleash bean
            syncTogglesToUnleash()

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
                enableAllExcept(
                    *autoDisabledToggles.toTypedArray()
                )
            }.also {
                log.info { "FeatureToggleConfigLocal: Bruker LocalUnleash (ingen Unleash-server konfigurert)" }
            }
        }
    }

    /**
     * Synkroniserer alle toggles fra ToggleName til Unleash server.
     * Kjøres automatisk ved oppstart hvis Unleash er konfigurert.
     */
    private fun syncTogglesToUnleash() {
        try {
            val syncService = UnleashToggleSyncService(
                unleashUrl = unleashUrl,
                unleashAdminToken = unleashAdminToken,
                projectId = unleashProjectId,
                environment = unleashEnvironment,
                autoDisabledToggles = autoDisabledToggles
            )
            syncService.syncAllToggles()
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke synkronisere toggles til Unleash, fortsetter uansett" }
        }
    }
}
