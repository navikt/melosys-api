package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import no.nav.melosys.exception.TekniskException
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await

object AwaitUtil {
    fun <T> awaitWithFailOnLogErrors(
        timeoutHandler: (e: ConditionTimeoutException) -> T = { e -> throw TekniskException("Timeout ved await: ${e.message}") },
        block: ConditionFactory.(log: List<ILoggingEvent>) -> T
    ): T {
        return LoggingTestUtils.withLogCapture { logEvents ->
            try {
                await.throwOnLogError(logEvents).block(logEvents)
            } catch (e: ConditionTimeoutException) {
                timeoutHandler(e)
            }
        }
    }

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory {
        return this.conditionEvaluationListener {
            logEvents.firstOrNull { it.level == Level.ERROR }?.let {
                throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
            }
        }
    }
}
