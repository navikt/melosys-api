package no.nav.melosys

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import io.kotest.assertions.withClue
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

    fun <T> ConditionFactory.untilMatching(
        waitFor: T,
        clueMessages: (e: Exception) -> String? = { e -> e.message },
        getCurrent: () -> T?
    ) {
        try {
            until { getCurrent() == waitFor }
        } catch (e: ConditionTimeoutException) {
            withClue(clueMessages(e)) {
                getCurrent() shouldBe waitFor
            }
        }
    }

    fun ConditionFactory.throwOnLogError(logEvents: List<ILoggingEvent>): ConditionFactory = this.conditionEvaluationListener {
        logEvents.firstOrNull { it.level == Level.ERROR }?.let {
            throw TekniskException("Fant log entry med level error: ${it.formattedMessage}")
        }
    }

    fun <T> ConditionFactory.builder() = AwaitBuilder<T>(this)

    class AwaitBuilder<T>(private val conditionFactory: ConditionFactory) {
        private lateinit var waitFor: () -> Boolean
        private lateinit var assertIfTimeout: (e: ConditionTimeoutException) -> Unit
        fun waitFor(block: () -> Boolean) = apply {
            waitFor = block
        }

        fun assertIfTimeout(block: (e: ConditionTimeoutException) -> Unit) = apply {
            assertIfTimeout = block
        }

        fun execute() {
            try {
                conditionFactory.until {
                    waitFor()
                }
            } catch (e: ConditionTimeoutException) {
                assertIfTimeout(e)
            }
        }
    }
}
