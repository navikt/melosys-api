package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.AwaitUtil.throwOnLogError
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.filterBuilder
import no.nav.melosys.ProsessLaget
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.ProsessinstansFerdigListener
import no.nav.melosys.saksflyt.steg.behandling.OpprettManglendeInnbetalingBehandling
import no.nav.melosys.saksflyt.steg.brev.SendManglendeInnbetalingVarselBrev
import no.nav.melosys.saksflyt.steg.oppgave.OpprettOppgave
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.LåsReferanseFactory
import no.nav.melosys.saksflytapi.domain.ProsessSteg
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
import java.time.LocalDate

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
@Import(SaksflytLåsreferanseIT.TestConfig::class)
internal class SaksflytLåsreferanseIT(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessLaget: ProsessLaget
) : OracleTestContainerBase() {
    @AfterEach
    fun setUp() = prosessLaget.clear()

    @Test
    fun `ikke kjør OpprettManglendeInnbetalingBehandling samtidig`() {
        LoggingTestUtils.withLogCapture { logItems ->
            val fakturaserieReferanse = "01HHFM03YMHHQAVZ4SQF9Y29E4"

            val manglendeFakturabetalingMelding1 = ManglendeFakturabetalingMelding(
                fakturaserieReferanse = fakturaserieReferanse,
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = "23004119"
            )
            val manglendeFakturabetalingMelding2 = ManglendeFakturabetalingMelding(
                fakturaserieReferanse = fakturaserieReferanse,
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = "23004118"
            )
            val manglendeInnbetalingLås1 = LåsReferanseFactory.lagString(manglendeFakturabetalingMelding1)
            val manglendeInnbetalingLås2 = LåsReferanseFactory.lagString(manglendeFakturabetalingMelding2)

            prosessLaget.nyProsessLaget("manglendeInnbetaling-1-Prosess") {
                prosessinstansService.opprettManglendeInnbetalingProsessflyt(
                    manglendeFakturabetalingMelding1
                )
            }
            prosessLaget.nyProsessLaget("manglendeInnbetaling-2-Prosess") {
                prosessinstansService.opprettManglendeInnbetalingProsessflyt(
                    manglendeFakturabetalingMelding2
                )
            }

            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    logItems.filterBuilder.match<ProsessinstansFerdigListener>()
                        .build().last().formattedMessage.contains("Prosessinstans(er) på vent med samme gruppe-prefiks: []")
                }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessLaget.prosessIdStringToName())
                .replace(manglendeInnbetalingLås1, "<manglendeInnbetalingLås1>")
                .replace(manglendeInnbetalingLås2, "<manglendeInnbetalingLås2>")
                .check { next ->
                    next { it shouldBe "Starter behandling av prosessinstans <manglendeInnbetaling-1-Prosess> med lås <manglendeInnbetalingLås1>" }
                    next { it shouldBe "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Utfører steg OPPRETT_OPPGAVE for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Prosessinstans <manglendeInnbetaling-1-Prosess> behandlet ferdig" }
                    next { it shouldBe "Starter behandling av prosessinstans <manglendeInnbetaling-2-Prosess> med lås <manglendeInnbetalingLås2>" }
                    next { it shouldBe "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING for prosessinstans <manglendeInnbetaling-2-Prosess>" }
                    next { it shouldBe "Utfører steg OPPRETT_OPPGAVE for prosessinstans <manglendeInnbetaling-2-Prosess>" }
                    next { it shouldBe "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV for prosessinstans <manglendeInnbetaling-2-Prosess>" }
                    next { it shouldBe "Prosessinstans <manglendeInnbetaling-2-Prosess> behandlet ferdig" }
                }
        }
    }

    @Test
    fun `ikke kjør OpprettManglendeInnbetalingBehandling samtidig med samme låsreferanse`() {
        LoggingTestUtils.withLogCapture { logItems ->
            val fakturaserieReferanse = "01HHFM03YMHHQAVZ4SQF9Y29E4"

            val manglendeFakturabetalingMelding1 = ManglendeFakturabetalingMelding(
                fakturaserieReferanse = fakturaserieReferanse,
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = "23004119"
            )
            val manglendeInnbetalingLås1 = LåsReferanseFactory.lagString(manglendeFakturabetalingMelding1)

            prosessLaget.nyProsessLaget("manglendeInnbetaling-1-Prosess") {
                prosessinstansService.opprettManglendeInnbetalingProsessflyt(
                    manglendeFakturabetalingMelding1
                )
            }
            prosessLaget.nyProsessLaget("manglendeInnbetaling-1-Duplikat") {
                prosessinstansService.opprettManglendeInnbetalingProsessflyt(
                    manglendeFakturabetalingMelding1
                )
            }
            await.throwOnLogError(logItems)
                .timeout(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1))
                .until {
                    logItems.filterBuilder.match<ProsessinstansFerdigListener>()
                        .build().last().formattedMessage.contains("Prosessinstans(er) på vent med samme gruppe-prefiks: []")
                }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessLaget.prosessIdStringToName())
                .replace(manglendeInnbetalingLås1, "<manglendeInnbetalingLås1>")
                .check { next ->
                    next { it shouldBe "Starter behandling av prosessinstans <manglendeInnbetaling-1-Prosess> med lås <manglendeInnbetalingLås1>" }
                    next { it shouldBe "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Utfører steg OPPRETT_OPPGAVE for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV for prosessinstans <manglendeInnbetaling-1-Prosess>" }
                    next { it shouldBe "Prosessinstans <manglendeInnbetaling-1-Prosess> behandlet ferdig" }
                    next { it shouldBe "Starter behandling av prosessinstans <manglendeInnbetaling-1-Duplikat> med lås <manglendeInnbetalingLås1>" }
                    next { it shouldBe "Utfører steg OPPRETT_MANGLENDE_INNBETALING_BEHANDLING for prosessinstans <manglendeInnbetaling-1-Duplikat>" }
                    next { it shouldBe "Utfører steg OPPRETT_OPPGAVE for prosessinstans <manglendeInnbetaling-1-Duplikat>" }
                    next { it shouldBe "Utfører steg SEND_MANGLENDE_INNBETALING_VARSELBREV for prosessinstans <manglendeInnbetaling-1-Duplikat>" }
                    next { it shouldBe "Prosessinstans <manglendeInnbetaling-1-Duplikat> behandlet ferdig" }
                }
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
}
