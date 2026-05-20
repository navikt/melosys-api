package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import no.nav.melosys.Application
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.metrics.MetrikkerNavn
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.saksflyt.PrioritertSaksflytTask
import no.nav.melosys.saksflyt.steg.sed.mottak.SedMottakRuting
import no.nav.melosys.saksflytapi.ProsessinstansForServiceRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

private val log = KotlinLogging.logger { }

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
@Disabled("Brukes lokalt for å teste at saksflytThreadPoolTaskExecutor (prioritetskøen) fungerer som forventet")
class SaksflyThreadPoolTaskExecutorIT(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessinstansTestManager: ProsessinstansTestManager,
    @Autowired private val prosessinstansRepo: ProsessinstansForServiceRepository,
    @Autowired private val meterRegistry: MeterRegistry,
    @Autowired @Qualifier("saksflytThreadPoolTaskExecutor") private val taskExecutor: ThreadPoolTaskExecutor
) : OracleTestContainerBase() {

    @AfterEach
    fun setUp() {
        ProsessinstansTestManager.timeOutFindingProsess = java.time.Duration.ofSeconds(30)
        prosessinstansTestManager.clear()
    }

    @Test
    fun `prosessinstanser kjøres til de er ferdige og køen tømmes`() {
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
        await.atMost(java.time.Duration.ofSeconds(2)).until {
            taskExecutor.threadPoolExecutor.queue.size == 0
        }
    }

    @Test
    fun `oppgaver i køen er innpakket som PrioritertSaksflytTask med prioritet`() {
        // Detaljert HØY-foran-LAV-ordning er dekket av PrioritertSaksflytTaskTest (rene enhetstester);
        // her bekreftes kun at den faktiske bønnen pakker sagaer som PrioritertSaksflytTask.
        prosessinstansTestManager.executeAndWait(
            mapOf(ProsessType.MOTTAK_SED to 10)
        ) {
            repeat(10) { i ->
                prosessinstansService.opprettProsessinstansSedMottak(MelosysEessiMelding().apply {
                    // rinaSaksnummer må være rent numerisk (LåsReferanseType.SED: ^\d+_[a-zA-Z0-9]+_\d+$);
                    // egen 1000-serie så det ikke kolliderer med rina-numrene i testet over (klassen rydder ikke DB mellom metodene).
                    rinaSaksnummer = (1000 + i).toString()
                    sedId = SedType.A009.name
                    sedVersjon = "1"
                })
            }
            Thread.sleep(100)
            taskExecutor.threadPoolExecutor.queue
                .filterIsInstance<PrioritertSaksflytTask>()
                .forEach { it.prioritet shouldBe ProsessPrioritet.NORMAL }
            loggKøMetrikker("kun NORMAL i kø")
        }
        await.atMost(java.time.Duration.ofSeconds(5)).until { taskExecutor.threadPoolExecutor.queue.size == 0 }
    }

    @Test
    fun `sub-prosess arver HØY-prioritet fra parent og dispatches som HØY`() {
        // Persistér en HØY parent direkte (uten dispatch via event) slik at findById i propageringen finner den.
        val parent = Prosessinstans.builder()
            .medType(ProsessType.MOTTAK_SED)
            .medStatus(ProsessStatus.KLAR)
            .medPrioritet(ProsessPrioritet.HØY)
            .medRegistrertDato(LocalDateTime.now())
            .medEndretDato(LocalDateTime.now())
            .build()
        val parentId = requireNotNull(prosessinstansRepo.save(parent).id)

        // Mett trådpoolen (corePoolSize=3) med trege NORMAL-oppgaver så barnet blir liggende i køen når vi sjekker.
        repeat(10) { i ->
            prosessinstansService.opprettProsessinstansSedMottak(sedMottak(2000 + i))
        }

        // Opprett en NORMAL sub-prosess i parentens kontekst -> skal løftes til HØY.
        val childId = medProsessKontekst(parentId) {
            prosessinstansService.opprettProsessinstansSedMottak(sedMottak(2999))
        }

        // Persisteringen bekrefter propagering + round-trip gjennom databasen.
        prosessinstansRepo.findById(childId).get().hentPrioritet() shouldBe ProsessPrioritet.HØY

        // Den faktiske executoren pakker barnet som en HØY PrioritertSaksflytTask.
        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            val childTask = taskExecutor.threadPoolExecutor.queue
                .filterIsInstance<PrioritertSaksflytTask>()
                .firstOrNull { it.prosessinstansId == childId }
            childTask?.prioritet shouldBe ProsessPrioritet.HØY
        }

        // Vis køfordelingen via de samme metrikkene som i prod (melosys.prosessinstanser.koe{prioritet=...}).
        loggKøMetrikker("sub-prosess løftet til HØY")

        await.atMost(Duration.ofSeconds(10)).until { taskExecutor.threadPoolExecutor.queue.size == 0 }
    }

    /**
     * Logger køfordelingen per prioritet via de samme Micrometer-målerne som eksponeres i prod
     * ([MetrikkerNavn.PROSESSINSTANSER_KØ] gauge, [MetrikkerNavn.PROSESSINSTANSER_KØ_AKTIVE] gauge og
     * [MetrikkerNavn.PROSESSINSTANSER_OPPRETTET] counter), slik at man ser fordelingen når testen kjøres lokalt.
     */
    private fun loggKøMetrikker(merkelapp: String) {
        val køPerPrioritet = ProsessPrioritet.entries.joinToString(", ") { prioritet ->
            val antall = meterRegistry.find(MetrikkerNavn.PROSESSINSTANSER_KØ)
                .tag(MetrikkerNavn.TAG_PRIORITET, prioritet.name).gauge()?.value()?.toInt() ?: 0
            "${prioritet.name}=$antall"
        }
        val aktiveTråder = meterRegistry.find(MetrikkerNavn.PROSESSINSTANSER_KØ_AKTIVE).gauge()?.value()?.toInt() ?: 0
        val opprettetMottakSed = meterRegistry.find(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET)
            .tag(MetrikkerNavn.TAG_TYPE, ProsessType.MOTTAK_SED.name).counter()?.count()?.toInt() ?: 0
        log.info { "[$merkelapp] kø per prioritet: [$køPerPrioritet], aktive tråder: $aktiveTråder, opprettet MOTTAK_SED (counter): $opprettetMottakSed" }
    }

    private fun sedMottak(rinaSaksnummer: Int) = MelosysEessiMelding().apply {
        // rinaSaksnummer må være rent numerisk (LåsReferanseType.SED), og unikt på tvers av testmetodene da DB ikke ryddes mellom dem.
        this.rinaSaksnummer = rinaSaksnummer.toString()
        sedId = SedType.A009.name
        sedVersjon = "1"
    }

    private fun <T> medProsessKontekst(parentId: UUID, block: () -> T): T {
        ThreadLocalAccessInfo.beforeExecuteProcess(parentId, "test-parent")
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(parentId)
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
