package no.nav.melosys.service

import tools.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
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

    private val isRunningAtomic = AtomicBoolean(false)

    private val isRunning: Boolean
        get() = isRunningAtomic.get()

    @Volatile
    private var startedAt: LocalDateTime? = null

    @Volatile
    private var stoppedAt: LocalDateTime? = null

    @Volatile
    var errorCount: Int = 0

    @Volatile
    var maxErrorsBeforeStop: Int = 0

    /**
     * Skrives fra jobbtråden mens `/status` kan serialisere samtidig fra en HTTP-tråd. En vanlig
     * HashMap kan da kaste ConcurrentModificationException eller gi en halvlest respons midt under
     * rehashing — nøyaktig når kartet er interessant, altså mens feil registreres.
     *
     * En synkronisert LinkedHashMap og ikke en ConcurrentHashMap: rapportene leses med den første
     * feilen først, og CHM har ingen rekkefølge. [status] kopierer under samme lås, så én respons er
     * ett bilde.
     */
    @Volatile
    var exceptions: MutableMap<String, Int> = Collections.synchronizedMap(LinkedHashMap())

    /**
     * Vakten mot to samtidige kjøringer. compareAndSet og ikke les-så-skriv: to kall som kommer inn
     * i samme øyeblikk — et dobbeltklikk eller en retry i et skript — ville ellers begge kunnet
     * passere, og for jobber som skriver betyr det at arbeidet gjøres to ganger.
     */
    fun execute(maxErrorsBeforeStop: Int = 0, block: T.() -> Unit) {
        this.maxErrorsBeforeStop = maxErrorsBeforeStop
        if (!isRunningAtomic.compareAndSet(false, true)) {
            log.warn("Job '$jobName' is already running.")
            return
        }
        startedAt = LocalDateTime.now()
        stoppedAt = null
        errorCount = 0
        exceptions.clear()
        stats.reset()
        return try {
            stats.block()
        } catch (ex: Exception) {
            log.error(ex) { "Job '$jobName' failed" }
            throw ex
        } finally {
            isRunningAtomic.set(false)
            shouldStop = false
            stoppedAt = LocalDateTime.now()
            log.info(
                "Job '$jobName' completed. Runtime: ${startedAt.durationUntil(stoppedAt)}" +
                    "\nStats: ${status().toJson()}"
            )
        }
    }

    fun registerException(e: Throwable) {
        val msg = e.message ?: e::class.simpleName ?: "Unknown error"
        synchronized(exceptions) { exceptions[msg] = exceptions.getOrDefault(msg, 0) + 1 }
        if (errorCount++ >= maxErrorsBeforeStop) {
            stop()
            log.error { "Stopping processing due to too many ($maxErrorsBeforeStop) errors" }
        }
    }

    fun stop() {
        log.info("Stopping job '$jobName' stats: ${status().toJson()}")
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
            "exceptions" to synchronized(exceptions) { LinkedHashMap(exceptions) }
        )

    private fun Any.toJson() = jacksonObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this)

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
