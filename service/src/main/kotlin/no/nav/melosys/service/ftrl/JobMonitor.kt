package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

class JobMonitor<T : JobMonitor.Stats>(
    private val jobName: String,
    val stats: T
) {
    private val shouldStopAtomic = AtomicBoolean(false)

    var shouldStop: Boolean
        get() = shouldStopAtomic.get()
        set(value) = shouldStopAtomic.set(value)

    @Volatile
    private var isRunning: Boolean = false

    @Volatile
    private var startedAt: LocalDateTime? = null

    @Volatile
    private var stoppedAt: LocalDateTime? = null

    @Volatile
    var errorCount: Int = 0

    @Volatile
    var maxErrorsBeforeStop: Int = 0

    @Volatile
    var exceptions: MutableMap<String, Int> = mutableMapOf()

    fun execute(maxErrorsBeforeStop: Int = 0, block: T.() -> Unit) {
        this.maxErrorsBeforeStop = maxErrorsBeforeStop
        if (isRunning) {
            log.warn("Job '$jobName' is already running.")
            return
        }
        isRunning = true
        startedAt = LocalDateTime.now()
        errorCount = 0
        exceptions.clear()
        stats.reset()
        return try {
            stats.block()
        } catch (ex: Exception) {
            log.error(ex) { "Job '$jobName' failed" }
            throw ex
        } finally {
            isRunning = false
            shouldStop = false
            stoppedAt = LocalDateTime.now()
            log.info("Job '$jobName' completed. Runtime: ${startedAt.durationUntil(stoppedAt)}")
        }
    }

    fun registerException(e: Throwable) {
        val msg = e.message ?: e::class.simpleName ?: "Unknown error"
        exceptions[msg] = exceptions.getOrDefault(msg, 0) + 1
        if (errorCount++ >= maxErrorsBeforeStop) {
            stop()
            log.error { "Stopping processing due to too many ($maxErrorsBeforeStop) errors" }
        }
    }

    fun stop() {
        log.info("Stopping job '$jobName' stats: ${status()}")
        shouldStop = true
    }

    fun status(): Map<String, Any?> =
        mapOf(
            "jobName" to jobName,
            "isRunning" to isRunning,
            "startedAt" to startedAt,
            "runtime" to startedAt.durationUntil(stoppedAt),
        ) + stats.asMap() + mapOf(
            "errorCount" to errorCount,
            "exceptions" to exceptions
        )

    fun durationUntil(other: LocalDateTime?): String = startedAt.durationUntil(other ?: LocalDateTime.now())

    private fun LocalDateTime?.durationUntil(other: LocalDateTime?): String =
        Duration.between(this ?: LocalDateTime.now(), other ?: LocalDateTime.now()).format()

    private fun Duration.format(): String =
        if (toMillis() < 1000) "${toMillis()} ms" else String.format("%.2f sec", toMillis() / 1000.0)

    interface Stats {
        fun reset()
        fun asMap(): Map<String, Any?>
    }
}
