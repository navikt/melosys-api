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
        logEvents.firstOrNull { it.level == Level.ERROR }?.let {
            throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
        }
    }
}
