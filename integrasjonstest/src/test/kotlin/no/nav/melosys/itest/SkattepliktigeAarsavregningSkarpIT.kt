package no.nav.melosys.itest

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.Application
import io.mockk.every
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflyt.ProsessinstansDispatcher
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.SkattehendelseDryrunItem
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.SkattepliktigeAarsavregningDryrunService
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.SkattepliktigeAarsavregningSkarpUtfoerer
import no.nav.melosys.service.sak.FagsakService
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Dekker transaksjonsgarantiene i skarp modus. De kan ikke bevises med mocks: de handler om hva som
 * faktisk står igjen i databasen når den ytre transaksjonen ryker.
 *
 * Den ytre kjøringen (`SkattepliktigeAarsavregningDryrunService`) er `@Transactional(readOnly = true)`
 * og spenner hele batchen, mens hver skriving kjører i `REQUIRES_NEW`. Testene her simulerer den
 * ytre transaksjonen med en read-only `TransactionTemplate` og ruller den tilbake til slutt.
 */
@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
class SkattepliktigeAarsavregningSkarpIT(
    @Autowired val skarpUtfoerer: SkattepliktigeAarsavregningSkarpUtfoerer,
    @Autowired val dryrunService: SkattepliktigeAarsavregningDryrunService,
    @Autowired val transactionManager: PlatformTransactionManager,
    @Autowired val jdbcTemplate: JdbcTemplate
) : OracleTestContainerBase() {

    /**
     * Testen bryr seg om radene, ikke om at saksflyten kjører dem. Uten denne ville
     * ProsessinstansOpprettetListener (AFTER_COMMIT) sendt instansene til executoren, som ville
     * feilet på saker vi aldri seeder — ERROR-støy i CI, og en åpen DML som kan gi ORA-00054 mot
     * TRUNCATE i neste tests truncateAllTables() (som svelger feilen med kun log.warn).
     */
    @MockkBean(relaxed = true)
    private lateinit var prosessinstansDispatcher: ProsessinstansDispatcher

    /** Sonde: første oppslag i løkka, brukt til å lese av transaksjonstilstanden inne i kjøringen. */
    @MockkBean(relaxed = true)
    private lateinit var fagsakService: FagsakService

    private val ytreLesetransaksjon: TransactionTemplate
        get() = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    /**
     * Den ytre transaksjonen ryker etter at to saker er skrevet. Med REQUIRED ville de vært
     * deltakere i den, og rullet tilbake sammen med den — mens rapporten fortsatt påsto at to
     * prosessinstanser var opprettet.
     */
    @Test
    fun `rollback av den ytre transaksjonen tar ikke med sakene som allerede er kjørt`() {
        ytreLesetransaksjon.execute { ytre ->
            skarpUtfoerer.opprettProsessinstans("MEL-901", "2023")
            skarpUtfoerer.opprettProsessinstans("MEL-903", "2023")

            ytre.setRollbackOnly()
            null
        }

        antallÅrsavregningsprosesser() shouldBe 2
    }

    /**
     * Den andre halvdelen av samme garanti: en sak som *kaster* skal ikke markere den ytre
     * transaksjonen rollback-only. Med REQUIRED ville kastet fra den indre gjort det, og de to
     * sakene rundt hadde forsvunnet ved commit selv om løkka fanget feilen og gikk videre.
     */
    @Test
    fun `en sak som kaster river ikke med seg sakene rundt`() {
        ytreLesetransaksjon.execute {
            skarpUtfoerer.opprettProsessinstans("MEL-901", "2023")
            // Behandlingen finnes ikke, så statusoppdateringen kaster inne i sin egen transaksjon.
            runCatching {
                skarpUtfoerer.settStatusVurderDokument(BEHANDLING_SOM_IKKE_FINNES, Behandlingsstatus.VURDER_DOKUMENT)
            }.isFailure shouldBe true
            skarpUtfoerer.opprettProsessinstans("MEL-903", "2023")
            null
        }

        antallÅrsavregningsprosesser() shouldBe 2
    }

    @Test
    fun `opprettelsen ber om innhentingsbrev helt ned i prosessinstansens data`() {
        ytreLesetransaksjon.execute {
            skarpUtfoerer.opprettProsessinstans("MEL-901", "2023")
            null
        }

        prosessdata("MEL-901", "2023") shouldContain "sendInnhentingsbrev=true"
    }

    /**
     * Pinner read-only-garantien på stien controlleren faktisk bruker.
     *
     * `prosesserSkattehendelserAsynkront` kaller `prosesserSkattehendelser` som selvkall, så
     * annotasjonen på den indre metoden er død config — hele garantien hviler på den ytre. I tillegg
     * har @Async- og @Transactional-advisorene begge LOWEST_PRECEDENCE; at async havner ytterst
     * skyldes at AsyncAnnotationBeanPostProcessor setter beforeExistingAdvisors=true, ikke en
     * ordning vi selv pinner. Resolves den motsatt vei, ville transaksjonen committet på
     * kalltråden og batchen kjørt helt uten transaksjon — da feiler denne testen på
     * `aktivTransaksjon`, ikke stille i prod.
     */
    @Test
    fun `batchen kjører i en aktiv read-only-transaksjon også gjennom Async-proxyen`() {
        val transaksjonstilstandLest = CountDownLatch(1)
        var readOnly: Boolean? = null
        var aktivTransaksjon: Boolean? = null

        every { fagsakService.hentFagsakerMedAktør(any(), any()) } answers {
            readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            aktivTransaksjon = TransactionSynchronizationManager.isActualTransactionActive()
            transaksjonstilstandLest.countDown()
            emptyList()
        }

        dryrunService.prosesserSkattehendelserAsynkront(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = "12345678901")),
            false,
            null,
        )

        transaksjonstilstandLest.await(10, TimeUnit.SECONDS) shouldBe true
        aktivTransaksjon shouldBe true
        readOnly shouldBe true
    }

    companion object {
        const val BEHANDLING_SOM_IKKE_FINNES = -1L
    }

    private fun antallÅrsavregningsprosesser(): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM prosessinstans WHERE prosess_type = 'OPPRETT_NY_BEHANDLING_AARSAVREGNING'",
        Int::class.java
    )!!

    /** Scopet på både sak og år: én sak kan ha flere årsavregningsprosesser. */
    private fun prosessdata(saksnummer: String, år: String): String = jdbcTemplate.queryForList(
        """SELECT dbms_lob.substr(data, 4000, 1) AS data FROM prosessinstans
           WHERE prosess_type = 'OPPRETT_NY_BEHANDLING_AARSAVREGNING'
             AND dbms_lob.instr(data, ?) > 0
             AND dbms_lob.instr(data, ?) > 0""",
        String::class.java,
        saksnummer,
        "gjelderÅr=$år"
    ).single()!!
}
