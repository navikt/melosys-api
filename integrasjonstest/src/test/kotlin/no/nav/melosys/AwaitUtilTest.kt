package no.nav.melosys

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import mu.KotlinLogging
import no.nav.melosys.AwaitUtil.awaitWithFailOnLogErrors
import no.nav.melosys.AwaitUtil.onTimeout
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.AwaitUtil.waitUntil
import no.nav.melosys.LoggingTestUtils.withLogCapture
import no.nav.melosys.exception.TekniskException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import java.time.Duration


class AwaitUtilTest {
    val log = KotlinLogging.logger { }

    @Test
    fun `awaitWithFailOnLogErrors skal avbryte await å kaste exception ved feil i log`() {
        val exception = shouldThrow<TekniskException> {
            awaitWithFailOnLogErrors {
                until {
                    log.error("error", TekniskException("feil i test"))
                    true
                }
            }
        }

        // Validate exact message format for single error with stacktrace
        exception.message.shouldNotBeNull().lines().take(4) shouldBe listOf(
            "Fant log entry med level error: error",
            "",
            "Stacktrace:",
            "no.nav.melosys.exception.TekniskException: feil i test"
        )
        // Rest of stacktrace lines follow with "at ..." format
    }


    @Test
    fun `awaitWithFailOnLogErrors skal finne log med stacktrace hvis dette finnes`() {
        val exception = shouldThrow<TekniskException> {
            awaitWithFailOnLogErrors {
                until {
                    log.error("første feil uten exception")
                    log.info("Ikke relevant")
                    log.error(
                        "feil med exception og da stacktrace",
                        TekniskException("feil i test", Throwable("underliggende feil"))
                    )
                    true
                }
            }
        }
        // Validate exact message format when multiple errors exist (one with stacktrace)
        val messageLines = exception.message.shouldNotBeNull().lines()

        // First section: list of all errors found
        messageLines.take(7) shouldBe listOf(
            "Fant 2 log entries med level error:",
            "- første feil uten exception",
            "- feil med exception og da stacktrace",
            "",
            "Rapporterer feil med stacktrace:",
            "Fant log entry med level error: feil med exception og da stacktrace",
            ""
        )

        // Second section: stacktrace details
        messageLines[7] shouldBe "Stacktrace:"
        messageLines[8] shouldBe "no.nav.melosys.exception.TekniskException: feil i test"
        // Rest contains stacktrace lines and cause chain

        messageLines.shouldContain("Caused by: java.lang.Throwable: underliggende feil")
    }

    @Test
    fun `awaitWithFailOnLogErrors skal fungere uten stacktrace`() {
        val exception = shouldThrow<TekniskException> {
            awaitWithFailOnLogErrors {
                until {
                    log.error("første feil uten exception")
                    log.info("Ikke relevant")
                    log.error("andre feil uten exception")
                    true
                }
            }
        }

        exception.message shouldBe listOf(
            "Fant 2 log entries med level error:",
            "- første feil uten exception",
            "- andre feil uten exception"
        ).joinToString("\n")
    }


    @Test
    fun `awaitWithFailOnLogErrors skal retunere verdi som lages av await`() {
        awaitWithFailOnLogErrors {
            untilNotNull {
                "not null"
            }
        }.shouldBe("not null")
    }


    @Test
    fun `withLogCapture med await-throwOnLogError skal avbryte await og kaste exception ved feil i log`() {
        shouldThrow<TekniskException> {
            withLogCapture { logEvents ->
                await.throwOnLogError(logEvents)
                    .until {
                        log.error("error")
                        true
                    }
            }
        }.message shouldContain "Fant log entry med level error: error"
    }

    @Test
    fun `await builder skal kjøre assert ved timeout - uten builder`() {
        shouldThrow<AssertionFailedError> {
            var i = 0
            await.atMost(Duration.ofMillis(2))
                .pollInterval(Duration.ofMillis(1))
                .pollDelay(Duration.ofMillis(1))
                .onTimeout { e ->
                    withClue("tall kan ikke være 1 - ${e.message}") {
                        i.toString() shouldBe "hei"
                    }
                }
                .waitUntil { i++.toString() == "hei" }
        }.message.shouldBe(
            "tall kan ikke være 1 - Condition with Lambda expression in no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
                "expected:<hei> but was:<1>"
        )
    }

    @Test
    fun `await builder skal kjøre assert ved abort`() {
        var i = 0
        shouldThrow<AssertionFailedError> {
            await.atMost(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(1))
                .pollDelay(Duration.ofMillis(1))
                .onTimeout { e ->
                    val message = if (e is AwaitUtil.ConditionAbortException) "waitUntil was aborted" else "waitUntil timed out"
                    withClue(message) {
                        "foo" shouldBe "bar"
                    }
                }
                .waitUntil(abort = { i == 5 }) {
                    i++ == 10
                }
        }.message.shouldBe(
            "waitUntil was aborted\nexpected:<bar> but was:<foo>"
        )
    }
}
