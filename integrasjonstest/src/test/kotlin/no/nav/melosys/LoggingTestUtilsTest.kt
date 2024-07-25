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

            logs.filterBuilder
                .match<SomeClass1> { it.message.contains("a-2") }
                .match<SomeClass2>() { it.message.contains("b-1") }
                .remove(Regex("-1"))
                .checkWithThreads { next ->
                    next("main").shouldBe("a-2")
                    next("main").shouldBe("b")
                }
        }
    }

    @Test
    fun `sort skal sorter ting på komma`() {
        val someOtherLog1 = LoggerFactory.getLogger(SomeClass1::class.java)

        LoggingTestUtils.withLogCapture { logs ->
            someOtherLog1.info("Prosessinstans(er) på vent med samme gruppe-prefiks: [<x008Prosess>, <a009Prosess>]")

            logs.filterBuilder
                .match<SomeClass1>()
                .sort(Regex("gruppe-prefiks: \\[(.*?)]"))
                .build().first().formattedMessage
                .shouldBe("Prosessinstans(er) på vent med samme gruppe-prefiks: [<a009Prosess>, <x008Prosess>]")
        }
    }

    @Test
    fun `should manage multiple threads without getting ConcurrentModificationException`() {
        val someClassLogger = LoggerFactory.getLogger(SomeClass1::class.java)

        val threadCount = 10
        val logMessageCount = 200
        repeat(10) { // Concurrent exceptions do not happen every time so run many times
            LoggingTestUtils.withLogCapture { logs ->
                val threads = mutableListOf<Thread>()
                repeat(threadCount) { threadNum ->
                    val thread = Thread {
                        repeat(logMessageCount) { i ->
                            someClassLogger.info("$threadNum - $i")
                        }
                    }
                    threads.add(thread)
                    thread.start()
                }


                var foundCount = 0
                while (true) {
                    if (logs.any { it.formattedMessage.contains("199") }) {
                        if (foundCount++ == threadCount) break
                    }
                    Thread.sleep(1) // To avid using all cpu
                }

                logs.filterBuilder
                    .match<SomeClass1>()
                    .build()
                    .count().shouldBe(threadCount * logMessageCount)

                threads.forEach { it.join() }
            }
        }
    }
}
