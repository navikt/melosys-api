package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.check
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.steg.behandling.OpprettManglendeInnbetalingBehandling
import no.nav.melosys.saksflyt.steg.brev.SendManglendeInnbetalingVarselBrev
import no.nav.melosys.saksflyt.steg.oppgave.OpprettOppgave
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.LåsReferanseFactory
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
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
import java.time.LocalDate

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
@Import(SaksflytLåsreferanseIT.TestConfig::class)
internal class SaksflytLåsreferanseIT(
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansService: ProsessinstansService,
) : OracleTestContainerBase() {

    @Test
    fun `ikke kjør OpprettManglendeInnbetalingBehandling samtidig`() {
        val logItems = LoggingTestUtils.captureLog<ProsessinstansBehandler> {

            val låsReferanser = lagProsesser(listOf("23004119", "23004118"))

            await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAll()
                        .filter { it.låsReferanse in låsReferanser }
                        .all { it.status == ProsessStatus.FERDIG }
                }
        }

        logItems.shouldHaveSize(10).check { next ->
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås OMIB_01HHFM03YMHHQAVZ4SQF9Y29E4_23004119")
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING"
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_OPPGAVE"
            next().formattedMessage shouldStartWith "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås OMIB_01HHFM03YMHHQAVZ4SQF9Y29E4_23004118")
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING"
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_OPPGAVE"
            next().formattedMessage shouldStartWith "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
        }
    }

    @Test
    fun `ikke kjør OpprettManglendeInnbetalingBehandling samtidig med samme låsreferanse`() {
        val logItems = LoggingTestUtils.captureLog<ProsessinstansBehandler> {

            val låsReferanser = lagProsesser(listOf("23004119", "23004119"))

            await.timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    prosessinstansRepository.findAll()
                        .filter { it.låsReferanse in låsReferanser }
                        .all { it.status == ProsessStatus.FERDIG }
                }
        }

        logItems.shouldHaveSize(10).check { next ->
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås OMIB_01HHFM03YMHHQAVZ4SQF9Y29E4_23004119")
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING"
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_OPPGAVE"
            next().formattedMessage shouldStartWith "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
            next().formattedMessage shouldMatch Regex("Starter behandling av prosessinstans .*? med lås OMIB_01HHFM03YMHHQAVZ4SQF9Y29E4_23004119")
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING"
            next().formattedMessage shouldStartWith "Utfører steg OPPRETT_OPPGAVE"
            next().formattedMessage shouldStartWith "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV"
            next().message shouldStartWith "Prosessinstans {} behandlet ferdig"
        }
    }


    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun opprettManglendeInnbetalingBehandlingForTest(): OpprettManglendeInnbetalingBehandling {
            return mockk<OpprettManglendeInnbetalingBehandling>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        @Primary
        fun opprettOpprettOppgaveForTest(): OpprettOppgave {
            return mockk<OpprettOppgave>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPRETT_OPPGAVE
                every { utfør(any()) } returns Unit
            }
        }

        @Bean
        @Primary
        fun opprettSendManglendeInnbetalingVarselBrevForTest(): SendManglendeInnbetalingVarselBrev {
            return mockk<SendManglendeInnbetalingVarselBrev>().apply {
                every { inngangsSteg() } returns ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV
                every { utfør(any()) } returns Unit
            }
        }
    }

    fun lagProsesser(fakturanummer: List<String>): List<String> {
        return fakturanummer.map {
            val manglendeFakturabetalingMelding = ManglendeFakturabetalingMelding(
                fakturaserieReferanse = "01HHFM03YMHHQAVZ4SQF9Y29E4",
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = it
            )
            prosessinstansService.opprettManglendeInnbetalingProsessflyt(manglendeFakturabetalingMelding)
            LåsReferanseFactory.lagStringFraManglendeFakturabetalingMelding(manglendeFakturabetalingMelding)
        }
    }
}
