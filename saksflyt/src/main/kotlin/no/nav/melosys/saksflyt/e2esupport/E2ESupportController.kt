package no.nav.melosys.saksflyt.e2esupport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.Prosessinstans
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

private const val POLL_INTERVAL_MS = 500L
private const val ERROR_MESSAGE_MAX_LENGTH = 500

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
    fun clearCaches(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(buildMap {
        put("jpa-first-level-cache", clearFirstLevelCache())
        put("jpa-second-level-cache", clearSecondLevelCache())
        put("spring-caches", clearSpringCaches())
    })

    @GetMapping("/process-instances/await")
    @Operation(summary = "Waits for all process instances to complete")
    fun awaitProcessInstances(
        @RequestParam(defaultValue = "30") timeoutSeconds: Long
    ): ResponseEntity<Map<String, Any>> {
        val startTime = Instant.now()
        val timeout = Duration.ofSeconds(timeoutSeconds)

        log.info { "Starting wait for prosessinstanser to complete (timeout: ${timeoutSeconds}s)" }

        return try {
            pollUntilComplete(startTime, timeout)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error(e) { "Interrupted while waiting for prosessinstanser" }
            buildInterruptedResponse(startTime, e)
        } catch (e: Exception) {
            log.error(e) { "Error while waiting for prosessinstanser" }
            buildErrorResponse(startTime, e)
        }
    }

    private fun clearFirstLevelCache(): String = runCatching {
        entityManager.clear()
        log.info { "Cleared JPA first-level cache (EntityManager)" }
        "cleared"
    }.getOrElse { e ->
        log.error(e) { "Failed to clear JPA first-level cache" }
        "error: ${e.message}"
    }

    private fun clearSecondLevelCache(): String = runCatching {
        entityManager.entityManagerFactory.cache.evictAll()
        log.info { "Cleared JPA second-level cache (Hibernate)" }
        "cleared"
    }.getOrElse { e ->
        log.error(e) { "Failed to clear JPA second-level cache" }
        "error: ${e.message}"
    }

    private fun clearSpringCaches(): String = runCatching {
        cacheManager?.let { manager ->
            val cacheNames = manager.cacheNames.toList()
            cacheNames.forEach { cacheName ->
                manager.getCache(cacheName)?.clear()
            }
            log.info { "Cleared Spring caches: $cacheNames" }
            "cleared: $cacheNames"
        } ?: run {
            log.warn { "No CacheManager bean found" }
            "no cache manager found"
        }
    }.getOrElse { e ->
        log.error(e) { "Failed to clear Spring caches" }
        "error: ${e.message}"
    }

    private fun pollUntilComplete(startTime: Instant, timeout: Duration): ResponseEntity<Map<String, Any>> {
        while (Duration.between(startTime, Instant.now()) < timeout) {
            val status = checkProcessStatus()

            log.debug {
                "Status: activeThreads=${status.activeThreads}, queueSize=${status.queueSize}, " +
                    "total=${status.allInstances.size}, notFinished=${status.notFinished.size}, failed=${status.failed.size}"
            }

            when {
                status.failed.isNotEmpty() -> return buildFailedResponse(startTime, status.failed)
                status.isComplete() -> return buildCompletedResponse(startTime, status.allInstances)
            }

            Thread.sleep(POLL_INTERVAL_MS)
        }

        return buildTimeoutResponse(startTime, timeout.seconds)
    }

    private fun checkProcessStatus(): ProcessStatus {
        val allInstances = prosessinstansRepository.findAll().toList()
        val notFinished = allInstances.filterNot { it.status == ProsessStatus.FERDIG }
        val failed = notFinished.filter { it.status == ProsessStatus.FEILET }

        return ProcessStatus(
            activeThreads = taskExecutor.activeCount,
            queueSize = taskExecutor.threadPoolExecutor.queue.size,
            allInstances = allInstances,
            notFinished = notFinished,
            failed = failed
        )
    }

    private fun buildFailedResponse(
        startTime: Instant,
        failed: List<Prosessinstans>
    ): ResponseEntity<Map<String, Any>> {
        log.error { "Found ${failed.size} failed prosessinstanser" }

        val failureDetails = failed.map { prosess ->
            val lastError = prosess.hendelser
                .filter { it.type != null }
                .maxByOrNull { it.dato }

            buildMap {
                put("id", prosess.id.toString())
                put("type", prosess.type.name)
                put("status", prosess.status.name)
                put("sistFullførtSteg", prosess.sistFullførtSteg?.name)
                put("error", buildMap {
                    put("type", lastError?.type)
                    put("steg", lastError?.steg?.name)
                    put("melding", lastError?.melding?.take(ERROR_MESSAGE_MAX_LENGTH))
                    put("dato", lastError?.dato.toString())
                })
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildMap {
            put("status", "FAILED")
            put("message", "Found ${failed.size} failed process instance(s)")
            put("failedInstances", failureDetails)
            put("elapsedSeconds", Duration.between(startTime, Instant.now()).seconds)
        })
    }

    private fun buildCompletedResponse(
        startTime: Instant,
        allInstances: List<Prosessinstans>
    ): ResponseEntity<Map<String, Any>> {
        val elapsed = Duration.between(startTime, Instant.now())
        log.info { "All prosessinstanser completed successfully in ${elapsed.seconds}s" }

        return ResponseEntity.ok(buildMap {
            put("status", "COMPLETED")
            put("message", "All process instances completed successfully")
            put("totalInstances", allInstances.size)
            put("elapsedSeconds", elapsed.seconds)
        })
    }

    private fun buildTimeoutResponse(startTime: Instant, timeoutSeconds: Long): ResponseEntity<Map<String, Any>> {
        val status = checkProcessStatus()

        log.warn {
            "Timeout reached after ${timeoutSeconds}s: " +
                "activeThreads=${status.activeThreads}, queueSize=${status.queueSize}, " +
                "notFinished=${status.notFinished.size}/${status.allInstances.size}"
        }

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(buildMap {
            put("status", "TIMEOUT")
            put("message", "Timeout after ${timeoutSeconds}s waiting for process instances to complete")
            put("activeThreads", status.activeThreads)
            put("queueSize", status.queueSize)
            put("totalInstances", status.allInstances.size)
            put("notFinished", status.notFinished.size)
            put("notFinishedIds", status.notFinished.map { it.id.toString() })
            put("elapsedSeconds", Duration.between(startTime, Instant.now()).seconds)
        })
    }

    private fun buildInterruptedResponse(startTime: Instant, e: Exception): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildMap {
            put("status", "INTERRUPTED")
            put("message", "Interrupted while waiting: ${e.message}")
            put("elapsedSeconds", Duration.between(startTime, Instant.now()).seconds)
        })

    private fun buildErrorResponse(startTime: Instant, e: Exception): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildMap {
            put("status", "ERROR")
            put("message", "Error while waiting: ${e.message}")
            put("elapsedSeconds", Duration.between(startTime, Instant.now()).seconds)
        })

    private data class ProcessStatus(
        val activeThreads: Int,
        val queueSize: Int,
        val allInstances: List<Prosessinstans>,
        val notFinished: List<Prosessinstans>,
        val failed: List<Prosessinstans>
    ) {
        fun isComplete(): Boolean = activeThreads == 0 && queueSize == 0 && notFinished.isEmpty()
    }
}
