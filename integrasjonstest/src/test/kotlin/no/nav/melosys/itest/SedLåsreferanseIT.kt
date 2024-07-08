package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.LoggingTestUtils.filterBuilder
import no.nav.melosys.ProsessRegister
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.ProsessinstansBehandler
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
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
    @Autowired private val prosessRegister: ProsessRegister,
    @Autowired private val prosessinstansTestManager: ProsessinstansTestManager
) : OracleTestContainerBase() {
    @AfterEach
    fun afterEach() {
        prosessRegister.clear()
        prosessinstansTestManager.clear()
    }

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

            prosessinstansTestManager.executeAndWait(
                mapOf(
                    ProsessType.MOTTAK_SED to 2
                )
            ) {
                prosessRegister.registrer("sed1Prosess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }
                prosessRegister.registrer("sed2Prosess") { prosessinstansService.opprettProsessinstansSedMottak(sed2) }
            }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessRegister.prosessIdStringToName())
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

            prosessinstansTestManager.executeAndWait(
                mapOf(
                    ProsessType.MOTTAK_SED to 2
                )
            ) {
                prosessRegister.registrer("førsteProsess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }
                prosessRegister.registrer("duplikatProsess") { prosessinstansService.opprettProsessinstansSedMottak(sed1) }
            }

            logItems.filterBuilder
                .match<ProsessinstansBehandler>()
                .replace(prosessRegister.prosessIdStringToName())
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
