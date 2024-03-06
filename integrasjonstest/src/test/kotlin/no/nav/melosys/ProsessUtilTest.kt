package no.nav.melosys

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.saksflyt.ProsessinstansRepository
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
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        every { prosessinstansRepository.findAllAfterDate(any()) } answers {
            emptyList()
        }

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstansRepository).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT
            ) {
            }
        }.message shouldBe "wait for prosees type:JFR_KNYTT to start\n" +
            "Condition with no.nav.melosys.AwaitUtil\$AwaitUntilBuilder was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element JFR_KNYTT based on object equality; but the collection is []"
    }

    @Test
    fun `assert med beskrivelse om prosess med ønsket type ikke blir funnet i databasen`() {
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        every { prosessinstansRepository.findAllAfterDate(any()) } returns
            listOf(
                Prosessinstans().apply {
                    type = ProsessType.IVERKSETT_VEDTAK_EOS
                    status = ProsessStatus.KLAR
                }
            )

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstansRepository).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT
            ) {
            }
        }.message shouldBe "wait for prosees type:JFR_KNYTT to start\n" +
            "Condition with no.nav.melosys.AwaitUtil\$AwaitUntilBuilder was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element JFR_KNYTT based on object equality; but the collection is [IVERKSETT_VEDTAK_EOS]"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke finnes i databasen`() {
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        every { prosessinstansRepository.findAllAfterDate(any()) } answers {
            listOf(Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
            })
        }

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstansRepository).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT,
                alsoWaitForprosessType = listOf(ProsessType.IVERKSETT_VEDTAK_EOS)
            ) {
            }
        }.message shouldBe "also wait for prosessTypes: [IVERKSETT_VEDTAK_EOS]\n" +
            "wait for prosees type:IVERKSETT_VEDTAK_EOS to start\n" +
            "Condition with no.nav.melosys.AwaitUtil\$AwaitUntilBuilder was not fulfilled within 2 milliseconds.\n" +
            "Collection should contain element IVERKSETT_VEDTAK_EOS based on object equality; but the collection is [JFR_KNYTT]"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke får status ferdig`() {
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        every { prosessinstansRepository.findAllAfterDate(any()) } answers {
            listOf(
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
        }

        shouldThrow<AssertionError> {
            ProsessUtil(prosessinstansRepository).executeAndWait(
                waitForprosessType = ProsessType.JFR_KNYTT,
                alsoWaitForprosessType = listOf(ProsessType.IVERKSETT_VEDTAK_EOS)
            ) {
            }
        }.message shouldBe "also wait for prosessTypes: [IVERKSETT_VEDTAK_EOS]\n" +
            "wait for prosees type:IVERKSETT_VEDTAK_EOS to have status FERDIG\n" +
            "Condition with no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "Expected IVERKSETT_VEDTAK_EOS but actual was null"
    }


    @Test
    fun `skal vente til prosess er ferdig`() {
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        val randomUUID = UUID.randomUUID()
        every { prosessinstansRepository.findById(randomUUID) } returns Optional.of(Prosessinstans())
        every { prosessinstansRepository.findAllAfterDate(any()) } answers {
            listOf(
                Prosessinstans().apply {
                    id = randomUUID
                    type = ProsessType.JFR_KNYTT
                    status = ProsessStatus.FERDIG
                }
            )
        }

        ProsessUtil(prosessinstansRepository).executeAndWait(
            waitForprosessType = ProsessType.JFR_KNYTT
        ) {
        }
    }

    @Test
    fun `skal retunere uuid til ferdig prosess`() {
        val prosessinstansRepository = mockk<ProsessinstansRepository>()
        val randomUUID = UUID.randomUUID()
        val prosessinstans = Prosessinstans().apply {
            id = randomUUID
            type = ProsessType.JFR_KNYTT
            status = ProsessStatus.FERDIG
        }
        every { prosessinstansRepository.findById(randomUUID) } returns Optional.of(prosessinstans)
        every { prosessinstansRepository.findAllAfterDate(any()) } answers {
            listOf(
                prosessinstans
            )
        }

        ProsessUtil(prosessinstansRepository).executeAndWait(
            waitForprosessType = ProsessType.JFR_KNYTT
        ) {
        }.id shouldBe randomUUID
    }
}
