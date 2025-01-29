package no.nav.melosys.itest

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local", "teammelosys.fattetvedtak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@Import(SaksflyThreadPoolTaskExecutorIT.TestConfig::class)
@Disabled("Brukes lokalt for å teste at saksflytThreadPoolTaskExecutor fungerer som forventet")
class SaksflyThreadPoolTaskExecutorIT(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessinstansTestManager: ProsessinstansTestManager,
    @Autowired @Qualifier("saksflytThreadPoolTaskExecutor") private val taskExecutor: ThreadPoolTaskExecutor
) : OracleTestContainerBase() {

    @AfterEach
    fun setUp() {
        ProsessinstansTestManager.timeOutFindingProsess = java.time.Duration.ofSeconds(30)
        prosessinstansTestManager.clear()
    }

    @Test
    fun test() {
        val events = LoggingTestUtils.captureLog<ProsessinstansService> {
            prosessinstansTestManager.executeAndWait(
                mapOf(
                    ProsessType.MOTTAK_SED to 10
                )
            ) {
                for (i in 1..10) {
                    val msg = MelosysEessiMelding().apply {
                        this.rinaSaksnummer = i.toString()
                        sedId = SedType.A009.name
                        sedVersjon = "1"
                    }
                    prosessinstansService.opprettProsessinstansSedMottak(msg)
                }
            }
        }
        events.filter { it.formattedMessage.contains("Antall prosessinstanser i saksflytThreadPoolTaskExecutor") }.forEach {
            println(it.formattedMessage)
        }
        await.atMost(java.time.Duration.ofSeconds(2)).until {
            taskExecutor.threadPoolExecutor.queue.size == 0
        }
    }


    @TestConfiguration
    class TestConfig {
        @Bean
        fun opprettSedMottakRutingTest(): SedMottakRuting = mockk<SedMottakRuting>().apply {
            every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING
            every { utfør(any()) } answers {
                Thread.sleep(500)

            }
        }
    }
}
