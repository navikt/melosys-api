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

    fun ConditionFactory.waitFor(
        waitFor: () -> Boolean,
    ) {
        try {
            until { waitFor() }
            threadLocalOnTimeoutLambda.remove()
        } catch (e: ConditionTimeoutException) {
            threadLocalOnTimeoutLambda.get().invoke(e)
        }
    }

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory = this.conditionEvaluationListener {
        logEvents.firstOrNull { it.level == Level.ERROR }?.let {
            throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
        }
    }
}
