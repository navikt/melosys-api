package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import no.nav.melosys.exception.TekniskException
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await

object AwaitUtil {
    private var threadLocalOnTimeoutLambda: ThreadLocal<(RuntimeException) -> Unit> =
        ThreadLocal.withInitial { { e: RuntimeException -> throw e } }

    fun <T> awaitWithFailOnLogErrors(block: ConditionFactory.(log: List<ILoggingEvent>) -> T): T =
        LoggingTestUtils.withLogCapture { logEvents ->
            await.throwOnLogError(logEvents).block(logEvents)
        }

    fun ConditionFactory.onTimeout(onTimeout: (e: RuntimeException) -> Unit) = apply {
        threadLocalOnTimeoutLambda.set(onTimeout)
    }

    fun ConditionFactory.waitUntil(
        abort: () -> Boolean = { false },
        waitUntil: () -> Boolean
    ) {
        try {
            until {
                if (abort()) {
                    throw ConditionAbortException()
                }
                waitUntil()
            }
            threadLocalOnTimeoutLambda.remove()
        } catch (e: ConditionTimeoutException) {
            threadLocalOnTimeoutLambda.get().invoke(e)
        } catch (e: ConditionAbortException) {
            threadLocalOnTimeoutLambda.get().invoke(e)
        }
    }

    class ConditionAbortException : RuntimeException()

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory = this.conditionEvaluationListener {
        val errorLogs = logEvents.filter { it.level == Level.ERROR }

        // Prioritize error with stacktrace if available, otherwise use first error
        val errorToReport = errorLogs.firstOrNull { it.throwableProxy != null } ?: errorLogs.firstOrNull()

        errorToReport?.let {
            val errorMessage = buildString {
                // Check if we have multiple errors
                if (errorLogs.size > 1) {
                    appendLine("Fant ${errorLogs.size} log entries med level error:")
                    errorLogs.forEach { error ->
                        appendLine("- ${error.formattedMessage}")
                    }

                    // Only add "Rapporterer feil med stacktrace:" if we actually have a stacktrace
                    if (it.throwableProxy != null) {
                        appendLine("\nRapporterer feil med stacktrace:")
                        appendLine("Fant log entry med level error: ${it.formattedMessage}")
                    }
                } else {
                    // Single error - always show the standard message
                    appendLine("Fant log entry med level error: ${it.formattedMessage}")
                }

                // Add stacktrace if present
                it.throwableProxy?.let { throwableProxy ->
                    appendLine("\nStacktrace:")
                    appendLine(throwableProxy.className + ": " + throwableProxy.message)
                    throwableProxy.stackTraceElementProxyArray?.forEach { stackElement ->
                        appendLine("\tat $stackElement")
                    }

                    // Include cause chain if present
                    var cause = throwableProxy.cause
                    while (cause != null) {
                        appendLine("Caused by: ${cause.className}: ${cause.message}")
                        cause.stackTraceElementProxyArray?.take(5)?.forEach { stackElement ->
                            appendLine("\tat $stackElement")
                        }
                        appendLine("\t... ${(cause.stackTraceElementProxyArray?.size ?: 0) - 5} more")
                        cause = cause.cause
                    }
                }
            }
            // Remove trailing newline for cleaner output
            throw TekniskException(errorMessage.trimEnd())
        }
    }
}
