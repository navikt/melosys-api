package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import no.nav.melosys.exception.TekniskException
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await

object AwaitUtil {
    private var threadLocalOnTimeoutLambda: ThreadLocal<(ConditionTimeoutException) -> Unit> =
        ThreadLocal.withInitial { { e: ConditionTimeoutException -> throw e } }

    fun <T> awaitWithFailOnLogErrors(block: ConditionFactory.(log: List<ILoggingEvent>) -> T): T =
        LoggingTestUtils.withLogCapture { logEvents ->
            await.throwOnLogError(logEvents).block(logEvents)
        }

    fun ConditionFactory.onTimeout(onTimeout: (e: ConditionTimeoutException) -> Unit) = apply {
        threadLocalOnTimeoutLambda.set(onTimeout)
    }

    fun ConditionFactory.waitUntil(
        waitUntil: () -> Boolean,
    ) {
        try {
            until { waitUntil() }
            threadLocalOnTimeoutLambda.remove()
        } catch (e: ConditionTimeoutException) {
            threadLocalOnTimeoutLambda.get().invoke(e)
        }
    }

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory = this.conditionEvaluationListener {
        findWithErrors(logEvents)?.let {
            throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
        }
    }

    private fun findWithErrors(logEvents: List<ILoggingEvent>): ILoggingEvent? {
        for (i in 1..3) {
            try {
                return logEvents.firstOrNull { it.level == Level.ERROR }
            } catch (e: ConcurrentModificationException) {
                // Siden dette gjelder test er det raskere og prøve på nytt, en å synkronisere
                LoggingTestUtils.log.warn("ConcurrentModification during find last log message, retrying $i", e)
            }
        }
        return logEvents.firstOrNull { it.level == Level.ERROR }
    }
}
