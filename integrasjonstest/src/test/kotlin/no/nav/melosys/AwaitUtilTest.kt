package no.nav.melosys

import io.kotest.assertions.AssertionFailedError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.melosys.AwaitUtil.awaitWithFailOnLogErrors
import no.nav.melosys.AwaitUtil.onTimeout
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.AwaitUtil.waitFor
import no.nav.melosys.LoggingTestUtils.withLogCapture
import no.nav.melosys.exception.TekniskException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import java.time.Duration


class AwaitUtilTest {
    val log = KotlinLogging.logger { }

    @Test
    fun `awaitWithFailOnLogErrors skal avbryte await å kaste exception ved feil i log`() {
        shouldThrow<TekniskException> {
            awaitWithFailOnLogErrors {
                until {
                    log.error("error")
                    true
                }
            }
        }.message shouldBe "Fant log entry med level error: error"
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
        }.message shouldBe "Fant log entry med level error: error"
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
                .waitFor { i++.toString() == "hei" }
        }.message.shouldBe(
            "tall kan ikke være 1 - Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
                "expected:<\"hei\"> but was:<\"1\">"
        )
    }
}
