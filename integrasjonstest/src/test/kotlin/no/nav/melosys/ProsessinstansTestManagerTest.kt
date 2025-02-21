package no.nav.melosys

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis
import kotlin.test.Test


private val log = KotlinLogging.logger { }

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProsessinstansTestManagerTest {

    @BeforeEach
    fun setUp() {
        ProsessinstansTestManager.timeOut = Duration.ofMillis(2)
        ProsessinstansTestManager.timeOutFindingProsess = Duration.ofMillis(2)
        ProsessinstansTestManager.pollDelay = Duration.ofMillis(1)
        ProsessinstansTestManager.pollInterval = Duration.ofMillis(1)
    }

    @AfterAll
    fun tearDown() {
        ProsessinstansTestManager.reset()
    }

    @Test
    fun `vent til prosessinstanser er ferdige`() {
        ProsessinstansTestManager.reset()
        ProsessinstansTestManager.timeOut = Duration.ofMillis(2000)

        val jfrKnytt = Prosessinstans().apply {
            id = UUID.randomUUID()
            type = ProsessType.JFR_KNYTT
            status = ProsessStatus.KLAR
            registrertDato = LocalDateTime.now().plusMinutes(1)
        }
        val iverksettVedtakEos = Prosessinstans().apply {
            id = UUID.randomUUID()
            type = ProsessType.IVERKSETT_VEDTAK_EOS
            status = ProsessStatus.KLAR
            registrertDato = LocalDateTime.now().plusMinutes(1)
        }
        val prosessinstanser = CopyOnWriteArrayList<Prosessinstans>()
        var awaitCheckForDoneCount = 0

        val thread = Thread {
            Thread.sleep(10)
            log.info("prosessinstanser.add(jfrKnytt)")
            prosessinstanser.add(jfrKnytt)
            Thread.sleep(10)
            log.info("prosessinstanser.add(iverksettVedtakEos)")
            prosessinstanser.add(iverksettVedtakEos)

            Thread.sleep(600)
            log.info("jfrKnytt.status = ProsessStatus.FERDIG")
            jfrKnytt.status = ProsessStatus.FERDIG
            Thread.sleep(600)
            log.info("iverksettVedtakEos.status = ProsessStatus.FERDIG")
            iverksettVedtakEos.status = ProsessStatus.FERDIG
        }

        LoggingTestUtils.withLogCapture { logItems ->
            ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
                waitForProsesses = mapOf(
                    ProsessType.JFR_KNYTT to 1,
                    ProsessType.IVERKSETT_VEDTAK_EOS to 1
                ),
                onWaitUntil = {
                    awaitCheckForDoneCount++
                    log.info { "awaitCheckForDoneCount:$awaitCheckForDoneCount" }
                }
            ) {
                thread.start()
            }

            jfrKnytt.status shouldBe ProsessStatus.FERDIG
            iverksettVedtakEos.status shouldBe ProsessStatus.FERDIG

            logItems.map { it.threadName to it.formattedMessage }
                .fold(mutableListOf<Pair<String, String>>()) { acc, current ->
                    if (current.first == "awaitility-thread") {
                        if (acc.lastOrNull()?.first != "awaitility-thread") {
                            acc.add("awaitility-thread" to "awaitCheckForDoneCount")
                        }
                    } else {
                        acc.add(current)
                    }
                    acc
                } shouldBe listOf(
                thread.name to "prosessinstanser.add(jfrKnytt)",
                thread.name to "prosessinstanser.add(iverksettVedtakEos)",
                "main" to "prosessinstanser started [JFR_KNYTT, IVERKSETT_VEDTAK_EOS]",
                "awaitility-thread" to "awaitCheckForDoneCount",
                thread.name to "jfrKnytt.status = ProsessStatus.FERDIG",
                "awaitility-thread" to "awaitCheckForDoneCount",
                "main" to "JFR_KNYTT ferdig",
                "awaitility-thread" to "awaitCheckForDoneCount",
                thread.name to "iverksettVedtakEos.status = ProsessStatus.FERDIG",
                "awaitility-thread" to "awaitCheckForDoneCount",
                "main" to "IVERKSETT_VEDTAK_EOS ferdig"
            )

            awaitCheckForDoneCount shouldBeGreaterThan 5
            thread.join()
        }
    }

    @Test
    fun `assert med beskrivelse om prosess ikke finnes i databasen`() {
        shouldThrow<AssertionError> {
            ProsessinstansTestManager().executeAndWait(
                mapOf(
                    ProsessType.JFR_KNYTT to 1
                )
            ) {
            }
        }.message shouldBe "Values differed at keys \n" +
            "wait for {JFR_KNYTT=1} processes to start\n" +
            "waitUntil timed out\n" +
            "expected:<{\n" +
            "  JFR_KNYTT = 1\n" +
            "}> but was:<{}>"
    }

    @Test
    fun `assert med beskrivelse om prosess med ønsket type ikke blir funnet i databasen`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                type = ProsessType.IVERKSETT_VEDTAK_EOS
                status = ProsessStatus.KLAR
                registrertDato = LocalDateTime.now().plusMinutes(1)
            }
        )

        shouldThrow<AssertionError> {
            ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
                mapOf(
                    ProsessType.JFR_KNYTT to 1
                )
            ) {
            }
        }.message shouldBe "Values differed at keys IVERKSETT_VEDTAK_EOS\n" +
            "wait for {JFR_KNYTT=1} processes to start\n" +
            "waitUntil timed out\n" +
            "expected:<{\n" +
            "  JFR_KNYTT = 1\n" +
            "}> but was:<{\n" +
            "  IVERKSETT_VEDTAK_EOS = 1\n" +
            "}>"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke finnes i databasen`() {
        val prosessinstanser = mutableListOf(Prosessinstans().apply {
            id = UUID.randomUUID()
            type = ProsessType.JFR_KNYTT
            status = ProsessStatus.FERDIG
            registrertDato = LocalDateTime.now().plusMinutes(1)
        })

        shouldThrow<AssertionError> {
            ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
                mapOf(
                    ProsessType.JFR_KNYTT to 1,
                    ProsessType.IVERKSETT_VEDTAK_EOS to 1
                )
            ) {
            }
        }.message shouldBe "Values differed at keys \n" +
            "wait for {JFR_KNYTT=1, IVERKSETT_VEDTAK_EOS=1} processes to start\n" +
            "waitUntil timed out\n" +
            "expected:<{\n" +
            "  JFR_KNYTT = 1,\n" +
            "  IVERKSETT_VEDTAK_EOS = 1\n" +
            "}> but was:<{\n" +
            "  JFR_KNYTT = 1\n" +
            "}>"
    }

    @Test
    fun `assert med beskrivelse om ekstra prosess ikke får status ferdig`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.IVERKSETT_VEDTAK_EOS
                status = ProsessStatus.KLAR
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
        )

        shouldThrow<AssertionError> {
            ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
                mapOf(
                    ProsessType.JFR_KNYTT to 1,
                    ProsessType.IVERKSETT_VEDTAK_EOS to 1
                )
            ) {
            }
        }.message shouldBe "Wait for [JFR_KNYTT, IVERKSETT_VEDTAK_EOS]\n" +
            "wait for prosees type:IVERKSETT_VEDTAK_EOS to have status FERDIG\n" +
            "Condition with Lambda expression in no.nav.melosys.AwaitUtil was not fulfilled within 2 milliseconds.\n" +
            "prosess med type: IVERKSETT_VEDTAK_EOS har status KLAR\n" +
            "expected:<FERDIG> but was:<KLAR>"
    }


    @Test
    fun `skal vente til prosess er ferdig`() {
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            }
        )

        ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
            mapOf(
                ProsessType.JFR_KNYTT to 1
            )
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
            registrertDato = LocalDateTime.now().plusMinutes(1)
        })

        ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
            mapOf(
                ProsessType.JFR_KNYTT to 1
            )
        ) {
        }.id shouldBe randomUUID
    }

    @Test
    fun `skal retunere uuid til ferdig prosess - ny methode som tar map`() {
        val randomUUID = UUID.randomUUID()
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                id = randomUUID
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            }
        )
        ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
            mapOf(
                ProsessType.JFR_KNYTT to 2,
            )
        ) {
        }.also {
            it.id shouldBe randomUUID
        }

    }

    @Test
    fun `skal bruke abort nå flere prosessr en hva vi venter på lages`() {
        ProsessinstansTestManager.timeOut = Duration.ofMillis(20000)

        val randomUUID = UUID.randomUUID()
        val prosessinstanser = mutableListOf(
            Prosessinstans().apply {
                id = randomUUID
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.JFR_KNYTT
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
            Prosessinstans().apply {
                id = UUID.randomUUID()
                type = ProsessType.MOTTAK_SED
                status = ProsessStatus.FERDIG
                registrertDato = LocalDateTime.now().plusMinutes(1)
            },
        )
        measureTimeMillis {
            shouldThrow<AssertionError> {
                ProsessinstansTestManager(prosessinstanser, prosessinstanser).executeAndWait(
                    mapOf(
                        ProsessType.JFR_KNYTT to 2,
                        ProsessType.MOTTAK_SED to 1
                    )
                ) {
                }
            }.message.shouldBe(
                "Values differed at keys JFR_KNYTT\n" +
                    "wait for {JFR_KNYTT=2, MOTTAK_SED=1} processes to start\n" +
                    "waitUntil was aborted because because the number of created process instances (4) >  exceeds the expected total (3)\n" +
                    "expected:<{\n" +
                    "  JFR_KNYTT = 2,\n" +
                    "  MOTTAK_SED = 1\n" +
                    "}> but was:<{\n" +
                    "  JFR_KNYTT = 3,\n" +
                    "  MOTTAK_SED = 1\n" +
                    "}>"
            )

        }.shouldBeLessThan(100)
    }
}
