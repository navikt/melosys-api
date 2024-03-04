package no.nav.melosys

import io.kotest.assertions.AssertionFailedError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.melosys.AwaitUtil.awaitWithFailOnLogErrors
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.AwaitUtil.untilMatching
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
                atMost(Duration.ofSeconds(1)).until {
                    log.error("error")
                    true
                }
            }
        }.message shouldBe "Fant log entry med level error: error"
    }

    @Test
    fun `awaitWithFailOnLogErrors skal retunere verdi som lages av await`() {
        awaitWithFailOnLogErrors {
            atMost(Duration.ofSeconds(1)).untilNotNull {
                "not null"
            }
        }.shouldBe("not null")
    }


    @Test
    fun `withLogCapture med await-throwOnLogError skal avbryte await og kaste exception ved feil i log`() {
        shouldThrow<TekniskException> {
            withLogCapture { logEvents ->
                await.throwOnLogError(logEvents)
                    .atMost(Duration.ofSeconds(1)).until {
                        log.error("error")
                        true
                    }
            }
        }.message shouldBe "Fant log entry med level error: error"
    }

    @Test
    fun `untilMatching skal sammenlikne waitFor med lambda`() {
        await.atMost(Duration.ofMillis(101)).untilMatching(
            waitFor = "last log messge"
        ) {
            "last log messge"
        }
    }


    @Test
    fun `untilMatching skal gi assert på forskjellig resultat ved timeout`() {
        shouldThrow<AssertionFailedError> {
            await.atMost(Duration.ofMillis(101)).untilMatching("waiting for message") {
                "last log messge"
            }
        }.message.shouldBe(
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 101 milliseconds.\n" +
                "expected:<\"waiting for message\"> but was:<\"last log messge\">"
        )
    }

    @Test
    fun `untilMatching skal gi assert på forskjellig resultat ved timeout bruk withClue`() {
        shouldThrow<AssertionFailedError> {
            await.atMost(Duration.ofMillis(101)).untilMatching(
                waitFor = "waiting for message",
                clueMessages = { e -> "Did not match last log message: ${e.message}" }
            ) {
                "last log message"
            }
        }.message.shouldBe(
            "Did not match last log message: Condition with no.nav.melosys.AwaitUtil was not fulfilled within 101 milliseconds.\n" +
                "expected:<\"waiting for message\"> but was:<\"last log message\">"
        )
    }
}
