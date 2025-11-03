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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

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
    @Transactional
    @Operation(summary = "Clears all JPA and Spring caches (not thread-safe for concurrent calls)")
    fun clearCaches(): ResponseEntity<Map<String, String>> = synchronized(this) {
        ResponseEntity.ok(buildMap {
            put("jpa-first-level-cache", clearFirstLevelCache())
            put("jpa-second-level-cache", clearSecondLevelCache())
            put("spring-caches", clearSpringCaches())
        })
    }

    @GetMapping("/process-instances/await")
    @Operation(summary = "Waits for all process instances to complete")
    fun awaitProcessInstances(
        @RequestParam(defaultValue = "30") timeoutSeconds: Long,
        @RequestParam(required = false) expectedInstances: Int? = null
    ): ResponseEntity<Map<String, Any>> {
        val startTime = Instant.now()
        val timeout = Duration.ofSeconds(timeoutSeconds)

        log.info {
            "Starting wait for prosessinstanser to complete " +
                "(timeout: ${timeoutSeconds}s, expectedInstances: ${expectedInstances ?: "any"})"
        }

        return try {
            // Initial settling delay to allow transactions to commit and tasks to be submitted
            Thread.sleep(INITIAL_SETTLING_DELAY_MS)
            log.debug { "Initial settling delay of ${INITIAL_SETTLING_DELAY_MS}ms completed" }

            pollUntilComplete(startTime, timeout, expectedInstances)
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

    private fun pollUntilComplete(
        startTime: Instant,
        timeout: Duration,
        expectedInstances: Int?
    ): ResponseEntity<Map<String, Any>> {
        var hasSeenActiveInstances = false

        while (Duration.between(startTime, Instant.now()) < timeout) {
            val status = checkProcessStatus()

            // Track if we've ever seen active instances
            if (status.hasActiveInstances) {
                hasSeenActiveInstances = true
            }

            log.debug {
                "Status: activeThreads=${status.activeThreads}, queueSize=${status.queueSize}, " +
                    "recent=${status.recentInstances.size}, total=${status.allInstances.size}, " +
                    "notFinished=${status.notFinished.size}, failed=${status.failed.size}, " +
                    "hasSeenActive=$hasSeenActiveInstances"
            }

            when {
                status.failed.isNotEmpty() -> {
                    log.error { "Found ${status.failed.size} failed prosessinstanser" }
                    return buildFailedResponse(startTime, status.failed)
                }
                status.isComplete(hasSeenActiveInstances, expectedInstances) -> {
                    val completionReason = buildCompletionReason(status, hasSeenActiveInstances, expectedInstances)
                    log.info { "Completion criteria met: $completionReason" }
                    return buildCompletedResponse(startTime, status.allInstances)
                }
            }

            Thread.sleep(POLL_INTERVAL_MS)
        }

        return buildTimeoutResponse(startTime, timeout.seconds)
    }

    private fun buildCompletionReason(
        status: ProcessStatus,
        hasSeenActiveInstances: Boolean,
        expectedInstances: Int?
    ): String = buildString {
        append("threads=0, queue=0, notFinished=0")
        if (expectedInstances != null) {
            append(", expected=$expectedInstances instances found")
        }
        if (hasSeenActiveInstances) {
            append(", seen active instances")
        }
        append(", recent=${status.recentInstances.size}")
    }

    private fun checkProcessStatus(): ProcessStatus {
        val allInstances = prosessinstansRepository.findAll().toList()
        val cutoffTime = LocalDateTime.now().minusSeconds(RECENT_INSTANCE_CUTOFF_SECONDS)

        // Filter for recent instances only (ignore old test data)
        val recentInstances = allInstances.filter { it.registrertDato.isAfter(cutoffTime) }
        val notFinished = recentInstances.filterNot { it.status == ProsessStatus.FERDIG }
        val failed = notFinished.filter { it.status == ProsessStatus.FEILET }

        // Check if there are any active (non-finished, non-failed) instances
        val active = notFinished.filterNot { it.status == ProsessStatus.FEILET }

        return ProcessStatus(
            activeThreads = taskExecutor.activeCount,
            queueSize = taskExecutor.threadPoolExecutor.queue.size,
            allInstances = allInstances,
            recentInstances = recentInstances,
            notFinished = notFinished,
            failed = failed,
            hasActiveInstances = active.isNotEmpty() || taskExecutor.activeCount > 0 || taskExecutor.threadPoolExecutor.queue.isNotEmpty()
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
                    put("melding", lastError?.melding?.let { msg ->
                        if (msg.length > ERROR_MESSAGE_MAX_LENGTH) {
                            "${msg.take(ERROR_MESSAGE_MAX_LENGTH)}... (truncated, ${msg.length} total chars)"
                        } else {
                            msg
                        }
                    })
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
        val recentInstances: List<Prosessinstans>,
        val notFinished: List<Prosessinstans>,
        val failed: List<Prosessinstans>,
        val hasActiveInstances: Boolean
    ) {
        /**
         * Determines if all process instances are complete.
         *
         * Completion criteria:
         * 1. No active threads or queue items
         * 2. No unfinished recent instances
         * 3. If expectedInstances is specified, must have seen at least that many
         * 4. Must have seen active instances OR have finished instances (prevents false-positive from empty DB)
         */
        fun isComplete(hasSeenActiveInstances: Boolean, expectedInstances: Int?): Boolean {
            val threadsAndQueueEmpty = activeThreads == 0 && queueSize == 0
            val noUnfinishedInstances = notFinished.isEmpty()

            // If expected count specified, verify we have at least that many recent instances
            val expectedCountMet = expectedInstances?.let { recentInstances.size >= it } ?: true

            // If there are NO process instances at all (not even old ones), return complete immediately
            // This handles the "fresh start" case where the database is clean
            if (allInstances.isEmpty() && threadsAndQueueEmpty) {
                return true
            }

            // Prevent false-positive: if we've never seen any active work, don't claim completion
            // UNLESS expectedInstances is specified and met (explicit coordination)
            // OR we have recent instances (proves work was done even if it completed very quickly)
            val hasSeenWork = hasSeenActiveInstances ||
                             (expectedInstances != null && expectedCountMet) ||
                             recentInstances.isNotEmpty()

            return threadsAndQueueEmpty && noUnfinishedInstances && expectedCountMet && hasSeenWork
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val INITIAL_SETTLING_DELAY_MS = 200L
        private const val RECENT_INSTANCE_CUTOFF_SECONDS = 60L
        private const val ERROR_MESSAGE_MAX_LENGTH = 500
    }
}
