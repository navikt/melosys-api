package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import io.kotest.matchers.shouldBe
import no.nav.melosys.exception.TekniskException
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await

object AwaitUtil {
    fun <T> awaitWithFailOnLogErrors(block: ConditionFactory.(log: List<ILoggingEvent>) -> T): T =
        LoggingTestUtils.withLogCapture { logEvents ->
            await.throwOnLogError(logEvents).block(logEvents)
        }

    fun <T> ConditionFactory.untilMatching(waitFor: T, getCurrent: () -> T?) {
        try {
            until {
                getCurrent() == waitFor
            }
        } catch (_: ConditionTimeoutException) {
            getCurrent() shouldBe waitFor
        }
    }

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory = this.conditionEvaluationListener {
        logEvents.firstOrNull { it.level == Level.ERROR }?.let {
            throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
        }
    }
}
