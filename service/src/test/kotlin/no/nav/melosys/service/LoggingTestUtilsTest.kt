package no.nav.melosys.service

import ch.qos.logback.classic.Level
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

class LoggingTestUtilsTest {

    @Test
    fun `skal finne loggoppføringer og nullstille listAppender etter bruk`() {
        (0..10).forEach {
            LoggingTestUtils.withLogAppender<LoggingTestUtilsTest> { listAppender ->
                log.warn("entry-1")

                listAppender.list.shouldHaveSize(1)
                    .first()
                    .let {
                        it.level.shouldBe(Level.WARN)
                        it.message.shouldBe("entry-1")
                    }
            }
        }
    }
}
