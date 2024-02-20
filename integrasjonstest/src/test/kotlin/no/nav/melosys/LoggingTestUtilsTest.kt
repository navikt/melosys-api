package no.nav.melosys

import ch.qos.logback.classic.Level
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.melosys.LoggingTestUtils.filterBuilder
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

private val log = KotlinLogging.logger { }

class LoggingTestUtilsTest {
    @Test
    fun `skal finne loggoppføringer og nullstille listAppender etter bruk`() {
        (0..10).forEach {
            LoggingTestUtils.withLogCapture { logEvents ->
                log.warn("entry-$it")

                logEvents.single().run {
                    level.shouldBe(Level.WARN)
                    message.shouldBe("entry-$it")
                }
            }
        }
    }

    @Test
    fun `skal kunne returnere verdi fra withLogCapture lambda`() {
        val returnVerdi = LoggingTestUtils.withLogCapture { logEvents ->
            log.info("entry-1")
            log.info("entry-2")
            logEvents.shouldHaveSize(2)
            "skal returneres"
        }

        returnVerdi.shouldBe("skal returneres")
    }

    @Test
    fun `captureLog skal returnere log entries for gitt klasse`() {
        val someOtherLog = LoggerFactory.getLogger(AwaitUtil::class.java)

        LoggingTestUtils.captureLog<LoggingTestUtilsTest> {
            log.info("entry-1")
            log.info("entry-2")
            someOtherLog.info("skal ikke med ")
        }.shouldHaveSize(2)

        LoggingTestUtils.captureLog<AwaitUtil> {
            log.info("skal ikke med 1")
            log.info("skal ikke med 2")
            someOtherLog.info("skal med")
        }.single().formattedMessage.shouldBe("skal med")
    }

    class SomeClass1
    class SomeClass2

    @Test
    fun `filterBuilder skal filtrere`() {
        val someOtherLog1 = LoggerFactory.getLogger(SomeClass1::class.java)
        val someOtherLog2 = LoggerFactory.getLogger(SomeClass2::class.java)

        LoggingTestUtils.withLogCapture { logs ->
            someOtherLog1.info("a-1")
            someOtherLog1.info("a-2")
            someOtherLog2.info("b-1")
            someOtherLog2.info("b-2")

            logs.filterBuilder
                .match<SomeClass1> { it.message.contains("a-2") }
                .match<SomeClass2>() { it.message.contains("b-1") }
                .build()
                .shouldHaveSize(2).map { it.message }
                .shouldContainInOrder("a-2", "b-1")
        }
    }
}
