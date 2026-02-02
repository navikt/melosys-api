package no.nav.melosys.featuretoggle

import io.getunleash.Unleash
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.reflect.full.memberProperties

private val log = KotlinLogging.logger {}

@Component
class FeatureToggleStatusLogger(private val unleash: Unleash) {

    @EventListener(ApplicationReadyEvent::class)
    fun logToggleStatus() {
        log.info { "=== Feature Toggle Status ved oppstart ===" }

        try {
            val toggles = getToggleNamesFromObject()

            if (toggles.isEmpty()) {
                log.warn { "Ingen feature toggles funnet i ToggleName-objektet" }
                return
            }

            toggles.forEach { (fieldName, toggleName) ->
                val enabled = try {
                    unleash.isEnabled(toggleName)
                } catch (e: Exception) {
                    log.error(e) { "Feil ved sjekk av toggle '$toggleName'" }
                    null
                }

                when (enabled) {
                    true -> log.info { "  ✓ $fieldName ($toggleName): ENABLED" }
                    false -> log.info { "  ✗ $fieldName ($toggleName): DISABLED" }
                    null -> log.warn { "  ? $fieldName ($toggleName): FEIL VED SJEKK" }
                }
            }

            log.info { "=== Feature Toggle Status ferdig ===" }
        } catch (e: Exception) {
            log.error(e) { "Kunne ikke hente feature toggle status. Unleash-tilkobling kan ha feilet." }
        }
    }

    private fun getToggleNamesFromObject(): List<Pair<String, String>> {
        return ToggleName::class.memberProperties
            .filter { it.returnType.classifier == String::class }
            .mapNotNull { prop ->
                try {
                    val value = prop.get(ToggleName) as? String
                    if (value != null) prop.name to value else null
                } catch (e: Exception) {
                    null
                }
            }
    }
}
