package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import no.nav.melosys.Application
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.check
import no.nav.melosys.LoggingTestUtils.match
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Disabled
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
import java.lang.Thread.sleep
import java.time.Duration


private val log = KotlinLogging.logger { }

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
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansService: ProsessinstansService,
) : OracleTestContainerBase() {

    @Test
    fun `ikke kjør samtidig når sed har samme rinaSaksnummer men forsjellig sedId, sedVersjon`() {
        LoggingTestUtils.withLogCapture { logItems ->
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

            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAll()
                        .filter { it.låsReferanse in låsReferanser }
                        .all { it.status == ProsessStatus.FERDIG }
                }

            logItems.match<ProsessinstansBehandler>().shouldHaveSize(6).check { next ->
                next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås 111_222_1")
                next().formattedMessage shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
                next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
                next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås 111_222_2")
                next().formattedMessage shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
                next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
            }
        }
    }

    @Test
    // Test som viser dagens logikk, TODO dette bør også kjøre synkront som testen over
    // Er laget oppgave for å fikse dette: https://jira.adeo.no/browse/MELOSYS-6365 og da er denne testen grei å ha
    @Disabled("Denne testen feiler noen ganger siden den ikke er synkronisert, så log linjene kan komme i en annen rekkefølge")
    fun `kjør samtidig når sed har samme rinaSaksnummer, sedId og sedVersjon`() {
        val logItems = LoggingTestUtils.captureLog<ProsessinstansBehandler> {
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

        logItems.shouldHaveSize(6).check { next ->
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås 111_222_1")
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås 111_222_1")
            next().formattedMessage shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
            next().formattedMessage shouldStartWith "Utfører steg SED_MOTTAK_RUTING"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
        }
    }

    fun lagProsesser(eessiMeldinger: List<MelosysEessiMelding>): List<String> = eessiMeldinger.map {
        prosessinstansService.opprettProsessinstansSedMottak(it)
        sleep(10)
        it.lagUnikIdentifikator()
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun opprettSedMottakRutingTest(): SedMottakRuting {
            return mockk<SedMottakRuting>().apply {
                every { inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING

                val slot = CapturingSlot<Prosessinstans>()
                every { utfør(capture(slot)) } answers {
                    log.info("Utfører for prosess ${slot.captured.id} - ${slot.captured.låsReferanse}")
                }
            }
        }
    }
}
