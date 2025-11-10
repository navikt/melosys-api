package no.nav.melosys.featuretoggle

import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

private val log = KotlinLogging.logger {}

/**
 * Service for synkronisering av feature toggles til Unleash server.
 *
 * Denne klassen bruker Unleash Admin API for å:
 * - Opprette alle toggles fra ToggleName objektet
 * - Enable toggles i spesifisert environment (default: development)
 * - Håndtere auto-disabled toggles (toggles som ikke skal enablees automatisk)
 */
class UnleashToggleSyncService(
    private val unleashUrl: String,
    private val unleashAdminToken: String,
    private val projectId: String = "default",
    private val environment: String = "development",
    private val autoDisabledToggles: Set<String> = emptySet()
) {
    private val restTemplate = RestTemplate()

    /**
     * Henter alle toggle-navn fra ToggleName objektet ved å bruke reflection.
     */
    private fun getAllToggleNames(): List<String> {
        return ToggleName::class.java.declaredFields
            .filter { field ->
                field.type == String::class.java &&
                java.lang.reflect.Modifier.isStatic(field.modifiers) &&
                java.lang.reflect.Modifier.isFinal(field.modifiers)
            }
            .mapNotNull { field ->
                try {
                    field.get(null) as? String
                } catch (e: Exception) {
                    log.warn { "Kunne ikke hente verdi for field ${field.name}: ${e.message}" }
                    null
                }
            }
    }

    /**
     * Synkroniserer alle toggles til Unleash.
     * Oppretter toggles som ikke finnes og enabler dem i environment (unntatt auto-disabled toggles).
     */
    fun syncAllToggles() {
        if (unleashAdminToken.isBlank() || unleashUrl.isBlank()) {
            log.info { "UnleashToggleSyncService: Admin token eller URL mangler, hopper over synkronisering" }
            return
        }

        log.info { "UnleashToggleSyncService: Starter synkronisering av toggles til $unleashUrl" }

        val toggleNames = getAllToggleNames()
        log.info { "UnleashToggleSyncService: Fant ${toggleNames.size} toggles i ToggleName" }

        var created = 0
        var existing = 0
        var enabled = 0
        var failed = 0

        toggleNames.forEach { toggleName ->
            try {
                if (toggleExists(toggleName)) {
                    log.debug { "Toggle '$toggleName' finnes allerede" }
                    existing++
                } else {
                    createToggle(toggleName)
                    created++
                    log.info { "Opprettet toggle: $toggleName" }

                    // Enable toggle hvis den ikke er i auto-disabled listen
                    if (toggleName !in autoDisabledToggles) {
                        enableToggle(toggleName)
                        enabled++
                        log.info { "Enablet toggle: $toggleName i environment: $environment" }
                    } else {
                        log.info { "Toggle '$toggleName' er i auto-disabled listen, hopper over enabling" }
                    }
                }
            } catch (e: Exception) {
                failed++
                log.error(e) { "Feil ved synkronisering av toggle '$toggleName'" }
            }
        }

        log.info {
            "UnleashToggleSyncService: Synkronisering fullført - " +
            "Created: $created, Existing: $existing, Enabled: $enabled, Failed: $failed"
        }
    }

    /**
     * Sjekker om en toggle finnes i Unleash.
     */
    private fun toggleExists(toggleName: String): Boolean {
        val url = "$unleashUrl/admin/projects/$projectId/features/$toggleName"
        val headers = createHeaders()

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                String::class.java
            )
            response.statusCode == HttpStatus.OK
        } catch (e: HttpClientErrorException.NotFound) {
            false
        }
    }

    /**
     * Oppretter en ny toggle i Unleash.
     */
    private fun createToggle(toggleName: String) {
        val url = "$unleashUrl/admin/projects/$projectId/features"
        val headers = createHeaders()

        val requestBody = mapOf(
            "name" to toggleName,
            "type" to "release",
            "description" to "Auto-synced from ToggleName.kt",
            "impressionData" to false
        )

        restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(requestBody, headers),
            String::class.java
        )
    }

    /**
     * Enabler en toggle i spesifisert environment.
     */
    private fun enableToggle(toggleName: String) {
        val url = "$unleashUrl/admin/projects/$projectId/features/$toggleName/environments/$environment/on"
        val headers = createHeaders()

        restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity<Void>(headers),
            String::class.java
        )
    }

    /**
     * Lager HTTP headers med authorization token.
     */
    private fun createHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("Authorization", unleashAdminToken)
            set("Content-Type", "application/json")
        }
    }
}
