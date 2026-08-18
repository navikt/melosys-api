package no.nav.melosys.saksflyt.e2e.api

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
import org.springframework.beans.factory.annotation.Value
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
@Tag(
    name = "e2e-support",
    description = "Helper endpoints for automating end-to-end tests, including cache management and process instance monitoring. " +
        "Only available when running with local-mock profile."
)
class E2ESupportController(
    private val cacheManager: CacheManager?,
    private val prosessinstansRepository: ProsessinstansRepository,
    @Qualifier("saksflytThreadPoolTaskExecutor") private val taskExecutor: ThreadPoolTaskExecutor,
    private val e2eTestDataService: E2ETestDataService,
    @Value("\${melosys.e2e.initial-settling-delay-ms:200}") initialSettlingDelayMs: Long
) {
    /**
     * How long /await waits before its first look, for callers on the legacy contract (no marker).
     * Settable with `melosys.e2e.initial-settling-delay-ms`; setting it to 0 makes the race the marker
     * contract fixes reproducible on demand.
     */
    private val initialSettlingDelayMs: Long = initialSettlingDelayMs.coerceAtLeast(0)

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

    @GetMapping("/process-instances/marker")
    @Operation(
        summary = "Returns a marker (server timestamp) for use with the 'after' parameter on /await",
        description = "Take a marker BEFORE the action that starts a process, then call " +
            "/await?after=<marker>&expectedNew=<N>. That makes the wait race-free: instances registered " +
            "before the action can no longer satisfy it."
    )
    fun processInstanceMarker(): ResponseEntity<Map<String, Any>> {
        val marker = LocalDateTime.now()
        log.debug { "Handed out prosessinstans marker: $marker" }
        return ResponseEntity.ok(mapOf("marker" to marker.toString()))
    }

    @GetMapping("/process-instances/await")
    @Operation(
        summary = "Waits for process instances to complete",
        description = "Without 'after': waits for every instance registered in the last 60 seconds " +
            "(legacy contract — it cannot tell the caller's own work apart from the previous step's). " +
            "With 'after': waits until at least 'expectedNew' instances registered after the marker " +
            "exist AND all of them are FERDIG. Unfinished work from BEFORE the marker is then not " +
            "waited for; failures are still reported from the whole recent window, so a marker never " +
            "hides a backend failure. 'after' and 'expectedInstances' are mutually exclusive."
    )
    fun awaitProcessInstances(
        @RequestParam(defaultValue = "30") timeoutSeconds: Long,
        @RequestParam(required = false) expectedInstances: Int? = null,
        @RequestParam(required = false) after: String? = null,
        @RequestParam(required = false) expectedNew: Int? = null
    ): ResponseEntity<Map<String, Any>> {
        val startTime = Instant.now()
        val timeout = Duration.ofSeconds(timeoutSeconds)
        val criteria = try {
            AwaitCriteria.of(after, expectedNew, expectedInstances)
        } catch (e: IllegalArgumentException) {
            log.warn { "Rejected await request: ${e.message}" }
            return buildBadRequestResponse(e.message)
        }

        log.info { "Starting wait for prosessinstanser to complete (timeout: ${timeoutSeconds}s, $criteria)" }

        return try {
            // The settling delay gives transactions time to commit and tasks time to reach the executor.
            // When the caller demands new instances after a marker, that requirement subsumes the delay,
            // so we start polling right away. A pure drain (expectedNew=0) still needs it.
            if (!criteria.demandsNewInstances()) {
                Thread.sleep(initialSettlingDelayMs)
                log.debug { "Initial settling delay of ${initialSettlingDelayMs}ms completed" }
            }

            pollUntilComplete(startTime, timeout, criteria)
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
        criteria: AwaitCriteria
    ): ResponseEntity<Map<String, Any>> {
        var hasSeenActiveInstances = false
        // Marker-based waits start before the instance is even registered, and the common case resolves
        // within tens of milliseconds — so poll rapidly, then back off. The legacy contract keeps its
        // fixed cadence: polling it more often would widen its race, since a short window where the
        // previous step's work looks finished is exactly what makes it answer COMPLETED too early.
        var pollIntervalMs = if (criteria.after != null) INITIAL_POLL_INTERVAL_MS else MAX_POLL_INTERVAL_MS

        while (Duration.between(startTime, Instant.now()) < timeout) {
            val status = checkProcessStatus(criteria.after)

            // Track if we've ever seen active instances
            if (status.hasActiveInstances) {
                hasSeenActiveInstances = true
            }

            log.debug {
                "Status: activeThreads=${status.activeThreads}, queueSize=${status.queueSize}, " +
                    "recent=${status.recentInstances.size}, new=${status.newInstances.size}, " +
                    "total=${status.allInstances.size}, notFinished=${status.notFinished.size}, " +
                    "failed=${status.failed.size}, hasSeenActive=$hasSeenActiveInstances"
            }

            when {
                status.failed.isNotEmpty() -> {
                    log.error { "Found ${status.failed.size} failed prosessinstanser" }
                    return buildFailedResponse(startTime, status.failed)
                }

                status.isComplete(hasSeenActiveInstances, criteria) -> {
                    log.info { "Completion criteria met: ${buildCompletionReason(status, hasSeenActiveInstances, criteria)}" }
                    return buildCompletedResponse(startTime, status)
                }
            }

            Thread.sleep(pollIntervalMs)
            pollIntervalMs = (pollIntervalMs * 2).coerceAtMost(MAX_POLL_INTERVAL_MS)
        }

        return buildTimeoutResponse(startTime, timeout.seconds, criteria)
    }

    private fun buildCompletionReason(
        status: ProcessStatus,
        hasSeenActiveInstances: Boolean,
        criteria: AwaitCriteria
    ): String = buildString {
        append("threads=0, queue=0, notFinished=0")
        if (criteria.after != null) {
            // Only the marker rule was evaluated — expectedInstances cannot be combined with it.
            append(", ${status.newInstances.size} new instance(s) after ${criteria.after} all FERDIG")
            append(" (expectedNew=${criteria.expectedNew})")
            return@buildString
        }
        if (criteria.expectedInstances != null) {
            append(", expected=${criteria.expectedInstances} instances found")
        }
        if (hasSeenActiveInstances) {
            append(", seen active instances")
        }
        append(", recent=${status.recentInstances.size}")
    }

    private fun checkProcessStatus(after: LocalDateTime?): ProcessStatus {
        val allInstances = prosessinstansRepository.findAll().toList()
        val cutoffTime = LocalDateTime.now().minusSeconds(RECENT_INSTANCE_CUTOFF_SECONDS)

        // Filter for recent instances only (ignore old test data)
        val recentInstances = allInstances.filter { it.registrertDato.isAfter(cutoffTime) }

        // Instances registered after the caller's marker — the only ones that can be the caller's own work
        val newInstances = after?.let { marker -> allInstances.filter { it.registrertDato.isAfter(marker) } }.orEmpty()

        // Unfinished work is judged on the caller's own instances when a marker is given, on the
        // recent window otherwise. Failures are reported from both, so a marker never hides a
        // failure the legacy contract would have caught.
        val watched = if (after != null) newInstances else recentInstances
        val notFinished = watched.filterNot { it.status == ProsessStatus.FERDIG }
        val failed = (recentInstances + newInstances).distinctBy { it.id }.filter { it.status == ProsessStatus.FEILET }

        // Check if there are any active (non-finished, non-failed) instances
        val active = notFinished.filterNot { it.status == ProsessStatus.FEILET }

        return ProcessStatus(
            activeThreads = taskExecutor.activeCount,
            queueSize = taskExecutor.threadPoolExecutor.queue.size,
            allInstances = allInstances,
            recentInstances = recentInstances,
            newInstances = newInstances,
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
        status: ProcessStatus
    ): ResponseEntity<Map<String, Any>> {
        val elapsed = Duration.between(startTime, Instant.now())
        log.info { "All prosessinstanser completed successfully in ${elapsed.seconds}s" }

        return ResponseEntity.ok(buildMap {
            put("status", "COMPLETED")
            put("message", "All process instances completed successfully")
            put("totalInstances", status.allInstances.size)
            put("newInstances", status.newInstances.size)
            put("elapsedSeconds", elapsed.seconds)
        })
    }

    private fun buildTimeoutResponse(
        startTime: Instant,
        timeoutSeconds: Long,
        criteria: AwaitCriteria
    ): ResponseEntity<Map<String, Any>> {
        val status = checkProcessStatus(criteria.after)

        log.warn {
            "Timeout reached after ${timeoutSeconds}s: " +
                "activeThreads=${status.activeThreads}, queueSize=${status.queueSize}, " +
                "notFinished=${status.notFinished.size}/${status.allInstances.size}, " +
                "new=${status.newInstances.size} ($criteria)"
        }

        val message = if (criteria.after != null && status.newInstances.size < criteria.expectedNew!!) {
            "Timeout after ${timeoutSeconds}s: only ${status.newInstances.size} of ${criteria.expectedNew} " +
                "expected new process instance(s) were registered after ${criteria.after}"
        } else {
            "Timeout after ${timeoutSeconds}s waiting for process instances to complete"
        }

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(buildMap {
            put("status", "TIMEOUT")
            put("message", message)
            put("activeThreads", status.activeThreads)
            put("queueSize", status.queueSize)
            put("totalInstances", status.allInstances.size)
            put("newInstances", status.newInstances.size)
            put("notFinished", status.notFinished.size)
            put("notFinishedIds", status.notFinished.map { it.id.toString() })
            put("elapsedSeconds", Duration.between(startTime, Instant.now()).seconds)
        })
    }

    private fun buildBadRequestResponse(message: String?): ResponseEntity<Map<String, Any>> =
        ResponseEntity.badRequest().body(buildMap {
            put("status", "BAD_REQUEST")
            put("message", message ?: "Invalid parameters")
        })

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

    @PostMapping("/testdata/init")
    @Operation(
        summary = "Initialize test data for Playwright frontend integration tests",
        description = "Creates predefined test cases (MEL-1001 to MEL-1071) for frontend integration tests. " +
            "Idempotent - skips cases that already exist. " +
            "NOTE: This is for melosys-web Playwright tests against melosys-api with mocks, " +
            "not full E2E tests (see https://github.com/navikt/melosys-e2e-tests)."
    )
    fun initTestData(): ResponseEntity<Map<String, Any>> {
        log.info { "Received request to initialize e2e test data" }

        return try {
            val result = e2eTestDataService.initializeTestData()

            ResponseEntity.ok(buildMap {
                put("status", "OK")
                put("created", result.created)
                put("skipped", result.skipped)
                put("alreadyExisted", result.alreadyExisted)
                put("testFnr", E2ETestDataService.TEST_FNR)
                put("caseRange", "${E2ETestDataService.FIRST_CASE_ID} to ${E2ETestDataService.LAST_CASE_ID}")
                put(
                    "message", if (result.alreadyExisted) {
                        "Test data already existed - no changes made"
                    } else {
                        "Successfully initialized ${result.created} test cases"
                    }
                )
            })
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize test data" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildMap {
                put("status", "ERROR")
                put("message", "Failed to initialize test data: ${e.message}")
            })
        }
    }

    @PostMapping("/testdata/reset")
    @Operation(
        summary = "Reset test data for Playwright frontend integration tests",
        description = "Resets and creates predefined test cases (MEL-1001 to MEL-1071) for frontend integration tests. " +
            "NOTE: This is for melosys-web Playwright tests against melosys-api with mocks, " +
            "not full E2E tests (see https://github.com/navikt/melosys-e2e-tests)."
    )
    fun resetTestData(): ResponseEntity<Map<String, Any>> {
        log.info { "Received request to reset e2e test data" }

        return try {
            val result = e2eTestDataService.resetTestData()

            ResponseEntity.ok(buildMap {
                put("status", "OK")
                put("cleared", result.cleared)
                put("created", result.created)
                put("wasReset", result.wasReset)
                put("testFnr", E2ETestDataService.TEST_FNR)
                put("caseRange", "${E2ETestDataService.FIRST_CASE_ID} to ${E2ETestDataService.LAST_CASE_ID}")
                put("metadata", result.metadata)
                put(
                    "message", if (result.created > 0) {
                        "Successfully initialized ${result.created} test cases"
                    } else {
                        "Test cases already existed - no changes made"
                    }
                )
            })
        } catch (e: Exception) {
            log.error(e) { "Failed to reset test data" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildMap {
                put("status", "ERROR")
                put("message", "Failed to reset test data: ${e.message}")
            })
        }
    }

    /**
     * What the caller is waiting for.
     *
     * [after] is a marker taken before the action that starts the process. When set, only instances
     * registered after it count — which is what makes the wait race-free. [expectedNew] is then how
     * many such instances must exist (defaults to 1). `expectedNew=0` means "drain whatever has been
     * registered since the marker" without requiring anything new — used to clean up between tests.
     *
     * [expectedInstances] belongs to the legacy contract and means "at least N instances in the last
     * $RECENT_INSTANCE_CUTOFF_SECONDS seconds" — which the previous step's instances satisfy just as
     * well as the caller's own. Prefer [after].
     */
    private data class AwaitCriteria(
        val after: LocalDateTime?,
        val expectedNew: Int?,
        val expectedInstances: Int?
    ) {
        /** True when the caller waits for work that may not be registered yet. */
        fun demandsNewInstances(): Boolean = after != null && (expectedNew ?: DEFAULT_EXPECTED_NEW) > 0

        companion object {
            /**
             * Validates the parameter combination. Every rejection here is a case where the wait would
             * otherwise have looked like it was coordinating, but silently degraded to the racy contract.
             */
            fun of(after: String?, expectedNew: Int?, expectedInstances: Int?): AwaitCriteria {
                val marker = after?.let(::parseMarker)

                require(!(after != null && marker == null)) { "'after' must not be blank" }
                require(!(expectedNew != null && marker == null)) {
                    "'expectedNew' requires 'after' — without a marker there is nothing to count new instances from"
                }
                require(expectedNew == null || expectedNew >= 0) { "'expectedNew' must not be negative (was $expectedNew)" }
                require(!(marker != null && expectedInstances != null)) {
                    "'after' and 'expectedInstances' are mutually exclusive: 'expectedInstances' counts everything " +
                        "in the recent window, 'after' counts only instances registered after the marker"
                }

                return AwaitCriteria(
                    after = marker,
                    expectedNew = expectedNew ?: marker?.let { DEFAULT_EXPECTED_NEW },
                    expectedInstances = expectedInstances
                )
            }

            /**
             * Only the exact format handed out by /process-instances/marker is accepted. A value carrying
             * an offset or 'Z' is rejected rather than silently reinterpreted as server-local time — the
             * container clock is not necessarily the caller's, and a marker in the past would let every
             * pre-existing instance count as new.
             */
            private fun parseMarker(raw: String): LocalDateTime? {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) return null
                return runCatching { LocalDateTime.parse(trimmed) }.getOrElse {
                    throw IllegalArgumentException(
                        "'after' must be a local date-time exactly as returned by " +
                            "/internal/e2e/process-instances/marker (e.g. 2026-07-31T14:35:19.249748805), " +
                            "was '${trimmed.forEkko()}'"
                    )
                }
            }

            /**
             * Ekko av kallerens egen input i en 400-melding. Meldingen både logges og returneres,
             * så kontrolltegn strippes (et `%0A` i `after` ville ellers splittet logglinjen) og
             * lengden kappes. Endepunktet er `local-mock`-only, så dette er hygiene, ikke et
             * sikkerhetstiltak — men en avkortet melding er uansett den lesbare.
             */
            private fun String.forEkko(): String = filterNot { it.isISOControl() }
                .let { if (it.length > ECHOED_INPUT_MAX_LENGTH) "${it.take(ECHOED_INPUT_MAX_LENGTH)}... (truncated)" else it }
        }
    }

    private data class ProcessStatus(
        val activeThreads: Int,
        val queueSize: Int,
        val allInstances: List<Prosessinstans>,
        val recentInstances: List<Prosessinstans>,
        val newInstances: List<Prosessinstans>,
        val notFinished: List<Prosessinstans>,
        val failed: List<Prosessinstans>,
        val hasActiveInstances: Boolean
    ) {
        /**
         * Determines if the work the caller is waiting for is complete.
         *
         * With a marker ([AwaitCriteria.after]):
         * 1. No active threads or queue items
         * 2. At least expectedNew instances registered after the marker
         * 3. All of those are FERDIG
         *
         * Without a marker (legacy contract):
         * 1. No active threads or queue items
         * 2. No unfinished instances in the recent window
         * 3. If expectedInstances is specified, at least that many recent instances
         * 4. Must have seen active instances OR have recent instances (prevents false-positive from empty DB)
         */
        fun isComplete(hasSeenActiveInstances: Boolean, criteria: AwaitCriteria): Boolean {
            val threadsAndQueueEmpty = activeThreads == 0 && queueSize == 0
            val noUnfinishedInstances = notFinished.isEmpty()

            if (criteria.after != null) {
                // The marker IS the coordination: instances from before the action cannot satisfy this,
                // and an empty database cannot either.
                val expectedNewMet = newInstances.size >= (criteria.expectedNew ?: DEFAULT_EXPECTED_NEW)
                return threadsAndQueueEmpty && expectedNewMet && noUnfinishedInstances
            }

            // If expected count specified, verify we have at least that many recent instances
            val expectedCountMet = criteria.expectedInstances?.let { recentInstances.size >= it } ?: true

            // If there are NO process instances at all (not even old ones), return complete immediately
            // This handles the "fresh start" case where the database is clean
            if (allInstances.isEmpty() && threadsAndQueueEmpty) {
                return true
            }

            // Prevent false-positive: if we've never seen any active work, don't claim completion
            // UNLESS expectedInstances is specified and met (explicit coordination)
            // OR we have recent instances (proves work was done even if it completed very quickly)
            val hasSeenWork = hasSeenActiveInstances ||
                (criteria.expectedInstances != null && expectedCountMet) ||
                recentInstances.isNotEmpty()

            return threadsAndQueueEmpty && noUnfinishedInstances && expectedCountMet && hasSeenWork
        }
    }

    companion object {
        private const val INITIAL_POLL_INTERVAL_MS = 25L
        private const val MAX_POLL_INTERVAL_MS = 500L
        private const val DEFAULT_EXPECTED_NEW = 1
        private const val RECENT_INSTANCE_CUTOFF_SECONDS = 60L
        private const val ERROR_MESSAGE_MAX_LENGTH = 500
        private const val ECHOED_INPUT_MAX_LENGTH = 100
    }
}
