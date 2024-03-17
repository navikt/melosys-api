package no.nav.melosys

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.*
import kotlin.test.Test


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProsessUtilTest {

    @BeforeAll
    fun setUp() {
        ProsessUtil.timeOut = Duration.ofMillis(2)
        ProsessUtil.timeOutFindingProsess = Duration.ofMillis(2)
        ProsessUtil.pollDelay = Duration.ofMillis(1)
        ProsessUtil.pollInterval = Duration.ofMillis(1)
    }

    @AfterAll
    fun tearDown() {
        ProsessUtil.reset()
    }

    @Test
    fun `assert med beskrivelse om prosess ikke finnes i databasen`() {
        shouldThrow<AssertionError> {
            ProsessUtil().executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT
            ) {
            }
        }.message shouldBe "wait for process type:JFR_KNYTT to start\n" +
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element JFR_KNYTT based on object equality; but the collection is []"
    }

    @Test
    fun `assert med beskrivelse om prosess med ønsket type ikke blir funnet i databasen`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                type = ProsessType.IVERKSETT_VEDTAK_EOS
                status = ProsessStatus.KLAR
            }
        )

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstanser, prosessinstanser).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT
            ) {
            }
        }.message shouldBe "wait for process type:JFR_KNYTT to start\n" +
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element JFR_KNYTT based on object equality; but the collection is [IVERKSETT_VEDTAK_EOS]"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke finnes i databasen`() {
        val prosessinstanser = mutableListOf(Prosessinstans().apply {
            id = UUID.randomUUID()
            type = ProsessType.JFR_KNYTT
            status = ProsessStatus.FERDIG
        })

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstanser, prosessinstanser).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT,
                alsoWaitForprosessType = listOf(ProsessType.IVERKSETT_VEDTAK_EOS)
            ) {
            }
        }.message shouldBe "also wait for prosessTypes: [IVERKSETT_VEDTAK_EOS]\n" +
            "wait for process type:IVERKSETT_VEDTAK_EOS to start\n" +
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element IVERKSETT_VEDTAK_EOS based on object equality; but the collection is [JFR_KNYTT]"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke får status ferdig`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.IVERKSETT_VEDTAK_EOS
                status = ProsessStatus.KLAR
            },
        )

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstanser, prosessinstanser).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT,
                alsoWaitForprosessType = listOf(ProsessType.IVERKSETT_VEDTAK_EOS)
            ) {
            }
        }.message shouldBe "also wait for prosessTypes: [IVERKSETT_VEDTAK_EOS]\n" +
            "wait for prosees type:IVERKSETT_VEDTAK_EOS to have status FERDIG\n" +
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "prosess med type: IVERKSETT_VEDTAK_EOS har status KLAR\n" +
            "expected:<FERDIG> but was:<KLAR>"
    }


    @Test
    fun `skal vente til prosess er ferdig`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
            }
        )

        ProsessUtil(prosessinstanser, prosessinstanser).executeAndWait(
            waitForprosessType = ProsessType.JFR_KNYTT
        ) {
        }
    }

    @Test
    fun `skal retunere uuid til ferdig prosess`() {
        val randomUUID = UUID.randomUUID()
        val prosessinstanser = mutableListOf(Prosessinstans().apply {
            id = randomUUID
            type = ProsessType.JFR_KNYTT
            status = ProsessStatus.FERDIG
        })

        ProsessUtil(prosessinstanser, prosessinstanser).executeAndWait(
            waitForprosessType = ProsessType.JFR_KNYTT
        ) {
        }.id shouldBe randomUUID
    }
}
