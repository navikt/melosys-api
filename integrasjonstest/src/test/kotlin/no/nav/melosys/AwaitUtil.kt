package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import no.nav.melosys.exception.TekniskException
import org.awaitility.core.ConditionFactory
import org.awaitility.kotlin.await

object AwaitUtil {
    fun <T> awaitWithFailOnLogErrors(block: ConditionFactory.() -> T): T {
        return LoggingTestUtils.withLogCapture { logEvents ->
            await.throwOnLogError(logEvents).block()
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
