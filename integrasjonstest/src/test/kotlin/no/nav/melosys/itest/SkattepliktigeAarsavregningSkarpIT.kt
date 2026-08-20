package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.Application
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.SkattepliktigeAarsavregningSkarpUtfoerer
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
import org.springframework.transaction.support.TransactionTemplate

/**
 * Dekker transaksjonsgarantiene i skarp-modus for 8045-kjøringen. De kan ikke bevises med mocks:
 * de handler om hva som faktisk står igjen i databasen når den ytre transaksjonen ryker.
 *
 * Den ytre kjøringen (`SkattepliktigeAarsavregningDryrunService`) er `@Transactional(readOnly = true)`
 * og spenner hele batchen, mens hver side-effekt kjører i `REQUIRES_NEW`. Testene her simulerer den
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
    @Autowired val transactionManager: PlatformTransactionManager,
    @Autowired val jdbcTemplate: JdbcTemplate
) : OracleTestContainerBase() {

    private val ytreLesetransaksjon: TransactionTemplate
        get() = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    @Test
    fun `en sak som feiler ruller ikke tilbake sakene som allerede er kjørt`() {
        ytreLesetransaksjon.execute { ytre ->
            skarpUtfoerer.opprettProsessinstans("MEL-901", "2023")
            runCatching { error("sak MEL-902 feilet, slik vedtaksmetadata-feilen gjør i prod") }
            skarpUtfoerer.opprettProsessinstans("MEL-903", "2023")

            // Ytre transaksjon ryker — før fiksen tok den med seg alt arbeidet i batchen.
            ytre.setRollbackOnly()
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

        prosessdata("MEL-901") shouldContain "sendInnhentingsbrev=true"
    }

    private fun antallÅrsavregningsprosesser(): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM prosessinstans WHERE prosess_type = 'OPPRETT_NY_BEHANDLING_AARSAVREGNING'",
        Int::class.java
    )!!

    private fun prosessdata(saksnummer: String): String = jdbcTemplate.queryForObject(
        """SELECT dbms_lob.substr(data, 4000, 1) FROM prosessinstans
           WHERE prosess_type = 'OPPRETT_NY_BEHANDLING_AARSAVREGNING'
             AND dbms_lob.instr(data, ?) > 0""",
        String::class.java,
        saksnummer
    )!!
}
