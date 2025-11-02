package no.nav.melosys.saksflyt.e2esupport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant

private val log = KotlinLogging.logger { }

@Profile("local-mock")
@RestController
@Unprotected
@RequestMapping("/internal/e2e")
@Tag(name = "e2e-support", description = "E2E test support endpoints (local-mock profile only)")
class E2ESupportController(
    private val cacheManager: CacheManager?,
    private val prosessinstansRepository: ProsessinstansRepository,
    @Qualifier("saksflytThreadPoolTaskExecutor") private val taskExecutor: ThreadPoolTaskExecutor
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @PostMapping("/caches/clear")
    @Operation(summary = "Clears all JPA and Spring caches")
    fun clearCaches(): ResponseEntity<Map<String, String>> {
        val results = mutableMapOf<String, String>()

        // Clear JPA first-level cache (persistence context)
        try {
            entityManager.clear()
            results["jpa-first-level-cache"] = "cleared"
            log.info { "Cleared JPA first-level cache (EntityManager)" }
        } catch (e: Exception) {
            results["jpa-first-level-cache"] = "error: ${e.message}"
            log.error(e) { "Failed to clear JPA first-level cache" }
        }

        // Clear JPA second-level cache (Hibernate cache)
        try {
            val cache = entityManager.entityManagerFactory.cache
            cache.evictAll()
            results["jpa-second-level-cache"] = "cleared"
            log.info { "Cleared JPA second-level cache (Hibernate)" }
        } catch (e: Exception) {
            results["jpa-second-level-cache"] = "error: ${e.message}"
            log.error(e) { "Failed to clear JPA second-level cache" }
        }

        // Clear Spring caches
        try {
            if (cacheManager != null) {
                val cacheNames = cacheManager.cacheNames.toList()
                cacheNames.forEach { cacheName ->
                    cacheManager.getCache(cacheName)?.clear()
                }
                results["spring-caches"] = "cleared: $cacheNames"
                log.info { "Cleared Spring caches: $cacheNames" }
            } else {
                results["spring-caches"] = "no cache manager found"
                log.warn { "No CacheManager bean found" }
            }
        } catch (e: Exception) {
            results["spring-caches"] = "error: ${e.message}"
            log.error(e) { "Failed to clear Spring caches" }
        }

        return ResponseEntity.ok(results)
    }

    @GetMapping("/process-instances/await")
    @Operation(summary = "Waits for all process instances to complete")
    fun awaitProcessInstances(
        @RequestParam(defaultValue = "30") timeoutSeconds: Long
    ): ResponseEntity<Map<String, Any>> {
        val startTime = Instant.now()
        val timeout = Duration.ofSeconds(timeoutSeconds)

        log.info { "Starting wait for prosessinstanser to complete (timeout: ${timeoutSeconds}s)" }

        try {
            while (Duration.between(startTime, Instant.now()) < timeout) {
                // Check thread pool executor queue
                val activeThreads = taskExecutor.activeCount
                val queueSize = taskExecutor.threadPoolExecutor.queue.size

                // Check prosessinstans statuses
                val allInstances = prosessinstansRepository.findAll().toList()
                val notFinished = allInstances.filter { it.status != ProsessStatus.FERDIG }
                val failed = notFinished.filter { it.status == ProsessStatus.FEILET }

                log.debug {
                    "Status: activeThreads=$activeThreads, queueSize=$queueSize, " +
                        "total=${allInstances.size}, notFinished=${notFinished.size}, failed=${failed.size}"
                }

                // If there are failed instances, return immediately with error details
                if (failed.isNotEmpty()) {
                    val failureDetails = failed.map { prosess ->
                        val lastError = prosess.hendelser
                            .filter { it.type != null }
                            .maxByOrNull { it.dato }

                        mapOf(
                            "id" to prosess.id.toString(),
                            "type" to prosess.type.name,
                            "status" to prosess.status.name,
                            "sistFullførtSteg" to prosess.sistFullførtSteg?.name,
                            "error" to mapOf(
                                "type" to lastError?.type,
                                "steg" to lastError?.steg?.name,
                                "melding" to lastError?.melding?.take(500), // Limit error message length
                                "dato" to lastError?.dato.toString()
                            )
                        )
                    }

                    log.error { "Found ${failed.size} failed prosessinstanser" }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        mapOf(
                            "status" to "FAILED",
                            "message" to "Found ${failed.size} failed process instance(s)",
                            "failedInstances" to failureDetails,
                            "elapsedSeconds" to Duration.between(startTime, Instant.now()).seconds
                        )
                    )
                }

                // Check if all work is complete
                if (activeThreads == 0 && queueSize == 0 && notFinished.isEmpty()) {
                    val elapsed = Duration.between(startTime, Instant.now())
                    log.info { "All prosessinstanser completed successfully in ${elapsed.seconds}s" }
                    return ResponseEntity.ok(
                        mapOf(
                            "status" to "COMPLETED",
                            "message" to "All process instances completed successfully",
                            "totalInstances" to allInstances.size,
                            "elapsedSeconds" to elapsed.seconds
                        )
                    )
                }

                // Still waiting, sleep a bit
                Thread.sleep(500)
            }

            // Timeout reached
            val allInstances = prosessinstansRepository.findAll().toList()
            val notFinished = allInstances.filter { it.status != ProsessStatus.FERDIG }
            val activeThreads = taskExecutor.activeCount
            val queueSize = taskExecutor.threadPoolExecutor.queue.size

            log.warn {
                "Timeout reached after ${timeoutSeconds}s: " +
                    "activeThreads=$activeThreads, queueSize=$queueSize, " +
                    "notFinished=${notFinished.size}/${allInstances.size}"
            }

            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(
                mapOf(
                    "status" to "TIMEOUT",
                    "message" to "Timeout after ${timeoutSeconds}s waiting for process instances to complete",
                    "activeThreads" to activeThreads,
                    "queueSize" to queueSize,
                    "totalInstances" to allInstances.size,
                    "notFinished" to notFinished.size,
                    "notFinishedIds" to notFinished.map { it.id.toString() },
                    "elapsedSeconds" to Duration.between(startTime, Instant.now()).seconds
                )
            )

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error(e) { "Interrupted while waiting for prosessinstanser" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "INTERRUPTED",
                    "message" to "Interrupted while waiting: ${e.message}",
                    "elapsedSeconds" to Duration.between(startTime, Instant.now()).seconds
                )
            )
        } catch (e: Exception) {
            log.error(e) { "Error while waiting for prosessinstanser" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Error while waiting: ${e.message}",
                    "elapsedSeconds" to Duration.between(startTime, Instant.now()).seconds
                )
            )
        }
    }
}
