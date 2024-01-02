package no.nav.melosys.itest

import ch.qos.logback.classic.spi.ILoggingEvent
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansBehandlerDelegate
import no.nav.melosys.saksflyt.ProsessinstansFerdigListener
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class, SaksflytTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local", "teammelosys.fattetvedtak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
@Import(SedLåsreferanseIT.TestConfig::class)
internal class SedLåsreferanseIT(
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansService: ProsessinstansService,
) : OracleTestContainerBase() {
    private val processUUID = UUID.randomUUID()

    @BeforeEach
    fun before() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "test", "Z123456", "saksbehandler")
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }


    @Test
    fun `ikke kjør samtidig når sed har samme rinaSaksnummer men forsjellig sedId, sedVersjon`() {
        val logItems: List<ILoggingEvent> = withLogAppender {
            val låsReferanser = lagProsesser(
                listOf(
                    MelosysEessiMelding().apply {
                        rinaSaksnummer = "111"
                        sedId = "222"
                        sedVersjon = "1"
                    },
                    MelosysEessiMelding().apply {
                        rinaSaksnummer = "111"
                        sedId = "222"
                        sedVersjon = "2"
                    },
                )
            )

            await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAll()
                        .filter { it.låsReferanse in låsReferanser }
                        .all { it.status == ProsessStatus.FERDIG }
                }

        }

        logItems.shouldHaveSize(12).toList().check { formattedMessage, message ->
            formattedMessage() shouldMatch Regex("Starter behandling av prosessinstans [a-fA-F0-9\\\\-]+ med lås 111_222_1")
            formattedMessage() shouldMatch Regex("Prosessinstans [a-fA-F0-9\\\\-]+ med låsreferanse 111_222_2 satt på vent")
            formattedMessage() shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
            message() shouldStartWith "Prosessinstans {} behandlet ferdig"
            message() shouldStartWith "Prosessinstans {} ferdig"
            formattedMessage() shouldBe "Forsøker å starte neste prosessinstans, låsreferanse 111_222_1"
            formattedMessage() shouldMatch Regex("Prosessinstans [a-fA-F0-9\\\\-]+ med låsreferanse 111_222_2 startes opp etter å ha vært på vent")
            formattedMessage() shouldMatch Regex("Starter behandling av prosessinstans [a-fA-F0-9\\\\-]+ med lås 111_222_2")
            formattedMessage() shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
            message() shouldStartWith "Prosessinstans {} behandlet ferdig"
            message() shouldStartWith "Prosessinstans {} ferdig"
        }
    }

    @Test
    // Test som viser dagens logikk, TODO dette bør også kjøre synkront som testen over
    fun `kjør samtidig når sed har samme rinaSaksnummer, sedId og sedVersjon`() {
        val logItems = withLogAppender {
            val låsReferanser = lagProsesser(
                listOf(
                    MelosysEessiMelding().apply {
                        rinaSaksnummer = "111"
                        sedId = "222"
                        sedVersjon = "1"
                    },
                    MelosysEessiMelding().apply {
                        rinaSaksnummer = "111"
                        sedId = "222"
                        sedVersjon = "1"
                    },
                )
            )

            await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAll()
                        .filter { it.låsReferanse in låsReferanser }
                        .all { it.status == ProsessStatus.FERDIG }
                }
        }

        logItems.shouldHaveSize(10).toList().check { formattedMessage, message ->
            formattedMessage() shouldMatch Regex("Starter behandling av prosessinstans [a-fA-F0-9\\\\-]+ med lås 111_222_1")
            formattedMessage() shouldMatch Regex("Starter behandling av prosessinstans [a-fA-F0-9\\\\-]+ med lås 111_222_1")
            formattedMessage() shouldStartWith "Utfører steg SED_MOTTAK_RUTING for prosessinstans"
            formattedMessage() shouldStartWith "Utfører steg SED_MOTTAK_RUTING for prosessinstans"
            message() shouldBe "Prosessinstans {} behandlet ferdig"
            message() shouldBe "Prosessinstans {} behandlet ferdig"
            message() shouldBe "Prosessinstans {} ferdig"
            message() shouldBe "Prosessinstans {} ferdig"
            formattedMessage() shouldBe "Forsøker å starte neste prosessinstans, låsreferanse 111_222_1"
            formattedMessage() shouldBe "Forsøker å starte neste prosessinstans, låsreferanse 111_222_1"
        }
    }

    private fun lagProsesser(eessiMeldinger: List<MelosysEessiMelding>): List<String> = eessiMeldinger.map {
        prosessinstansService.opprettProsessinstansSedMottak(it)
        it.lagUnikIdentifikator()
    }

    private fun withLogAppender(block: () -> Unit): List<ILoggingEvent> = LoggingTestUtils.withLogAppender(
        ProsessinstansBehandler::class,
        ProsessinstansBehandlerDelegate::class,
        ProsessinstansFerdigListener::class
    ) {
        block()
    }

    private fun <R> List<ILoggingEvent>.check(block: (formattedMessage: () -> String, message: () -> String) -> R): R {
        var i = 0
        val formattedMessage = { this[i++].formattedMessage }
        val message = { this[i++].message }
        return block(formattedMessage, message)
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun opprettSedMottakRutingTest(): SedMottakRuting {
            return mockk<SedMottakRuting>().apply {
                every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING
                every { utfør(any()) } returns Unit
            }
        }
    }

}
