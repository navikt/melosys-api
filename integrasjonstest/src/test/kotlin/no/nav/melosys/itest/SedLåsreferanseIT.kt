package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.filterBuilder
import no.nav.melosys.ProsessLaget
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansFerdigListener
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class, SaksflytTestConfig::class],
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
@Import(SedLåsreferanseIT.TestConfig::class)
internal class SedLåsreferanseIT(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessLaget: ProsessLaget
) : OracleTestContainerBase() {
    @AfterEach
    fun setUp() = prosessLaget.clear()

    @Test
    fun `ikke kjør samtidig når sed har samme rinaSaksnummer men forsjellig sedId, sedVersjon`() {
        LoggingTestUtils.withLogCapture { logItems ->
            val sed1 = MelosysEessiMelding().apply {
                rinaSaksnummer = "111"
                sedId = "222"
                sedVersjon = "1"
            }
            val sed2 = MelosysEessiMelding().apply {
                rinaSaksnummer = "111"
                sedId = "222"
                sedVersjon = "2"
            }
            val sed1ås = sed1.lagUnikIdentifikator()
            val sed2ås = sed2.lagUnikIdentifikator()

            prosessLaget.nyProsessLaget("sed1Prosess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }
            prosessLaget.nyProsessLaget("sed2Prosess") { prosessinstansService.opprettProsessinstansSedMottak(sed2) }


            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    logItems.filterBuilder.match<ProsessinstansFerdigListener>()
                        .build().last().formattedMessage.contains("Prosessinstans(er) på vent med samme gruppe-prefiks: []")
                }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessLaget.prosessIdStringToName())
                .replace(sed1ås, "<sed1Lås>")
                .replace(sed2ås, "<sed2Lås>")
                .check { next ->
                    next { it shouldBe "Starter behandling av prosessinstans <sed1Prosess> med lås <sed1Lås>" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <sed1Prosess>" }
                    next { it shouldBe "Prosessinstans <sed1Prosess> behandlet ferdig" }
                    next { it shouldBe "Starter behandling av prosessinstans <sed2Prosess> med lås <sed2Lås>" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <sed2Prosess>" }
                    next { it shouldBe "Prosessinstans <sed2Prosess> behandlet ferdig" }
                }
        }
    }

    @Test
    fun `kjør samtidig når sed har samme rinaSaksnummer, sedId og sedVersjon`() {
        LoggingTestUtils.withLogCapture { logItems ->
            val sed1 = MelosysEessiMelding().apply {
                rinaSaksnummer = "111"
                sedId = "222"
                sedVersjon = "1"
            }
            val sed1ås = sed1.lagUnikIdentifikator()

            prosessLaget.nyProsessLaget("førsteProsess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }
            prosessLaget.nyProsessLaget("duplikatProsess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }

            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    logItems.filterBuilder.match<ProsessinstansFerdigListener>()
                        .build().last().formattedMessage.contains("Prosessinstans(er) på vent med samme gruppe-prefiks: []")
                }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessLaget.prosessIdStringToName())
                .replace(sed1ås, "<sed1Lås>")
                .check { next ->
                    next { it shouldBe "Starter behandling av prosessinstans <førsteProsess> med lås <sed1Lås>" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <førsteProsess>" }
                    next { it shouldBe "Prosessinstans <førsteProsess> behandlet ferdig" }
                    next { it shouldBe "Starter behandling av prosessinstans <duplikatProsess> med lås <sed1Lås>" }
                    next { it shouldBe "Utfører steg SED_MOTTAK_RUTING for prosessinstans <duplikatProsess>" }
                    next { it shouldBe "Prosessinstans <duplikatProsess> behandlet ferdig" }
                }
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun opprettSedMottakRutingTest(): SedMottakRuting {
            return mockk<SedMottakRuting>().apply {
                every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } returns Unit
            }
        }
    }
}
