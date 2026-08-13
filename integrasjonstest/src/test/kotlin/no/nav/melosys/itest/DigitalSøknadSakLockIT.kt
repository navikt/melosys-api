package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import no.nav.melosys.repository.DigitalSøknadSakLås
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Integrasjonstest for DB-låsen bak [DigitalSøknadSakLås] (MELOSYS-8151).
 *
 * Verifiserer mot ekte Oracle at `SELECT ... FOR UPDATE` på samme aktørId serialiserer to
 * transaksjoner: tråd B får ikke låsen før tråd A committer. Ulik aktørId skal ikke blokkere.
 *
 * Vi tester via [DigitalSøknadSakLås] og ikke mot repositoriet direkte, fordi det er bønnen
 * produksjonskoden bruker: den orkestrerer radopprettelse (REQUIRES_NEW) og radlås (MANDATORY)
 * som to separate proxy-kall, og håndterer samtidig radopprettelse.
 */
class DigitalSøknadSakLockIT(
    @Autowired private val sakLås: DigitalSøknadSakLås,
    @Autowired private val transactionManager: PlatformTransactionManager,
) : ComponentTestBase() {

    @Test
    fun `to transaksjoner på samme aktørId serialiseres til de committer`() {
        val aktørId = "laas-${UUID.randomUUID()}"

        val hendelser = CopyOnWriteArrayList<String>()
        val aHarLåsen = CountDownLatch(1)
        val aKanSlippe = CountDownLatch(1)

        val trådA = thread {
            tx {
                sakLås.lås(aktørId)
                hendelser.add("A-tok-lås")
                aHarLåsen.countDown()
                // Holdes godt under FOR UPDATE WAIT-grensen, slik at B venter og ikke gir opp.
                aKanSlippe.await(10, TimeUnit.SECONDS)
                hendelser.add("A-committer")
            }
        }

        aHarLåsen.await(10, TimeUnit.SECONDS)

        val trådB = thread {
            tx {
                sakLås.lås(aktørId)
                hendelser.add("B-tok-lås")
            }
        }

        // Gi B tid til å forsøke å ta låsen; den skal blokkere så lenge A holder den.
        Thread.sleep(2000)
        hendelser shouldNotContain "B-tok-lås"

        aKanSlippe.countDown()
        trådA.join(10_000)
        trådB.join(10_000)

        hendelser shouldContainExactly listOf("A-tok-lås", "A-committer", "B-tok-lås")
    }

    @Test
    fun `ulik aktørId blokkerer ikke`() {
        val aktørA = "laas-${UUID.randomUUID()}"
        val aktørB = "laas-${UUID.randomUUID()}"

        val bFikkLås = CountDownLatch(1)
        val aKanSlippe = CountDownLatch(1)

        val trådA = thread {
            tx {
                sakLås.lås(aktørA)
                aKanSlippe.await(10, TimeUnit.SECONDS)
            }
        }
        val trådB = thread {
            tx {
                sakLås.lås(aktørB)
                bFikkLås.countDown()
            }
        }

        // B skal få sin lås selv om A holder sin (ulik aktørId) — uten å vente på A.
        val bFikkLåsUtenÅVente = bFikkLås.await(5, TimeUnit.SECONDS)
        aKanSlippe.countDown()
        trådA.join(10_000)
        trådB.join(10_000)

        assert(bFikkLåsUtenÅVente) { "B skulle fått låsen på egen aktørId uten å vente på A" }
    }

    @Test
    fun `venter ikke i det uendelige på en opptatt lås`() {
        val aktørId = "laas-${UUID.randomUUID()}"

        val aHarLåsen = CountDownLatch(1)
        val aKanSlippe = CountDownLatch(1)
        val bFeil = AtomicReference<Throwable>()

        val trådA = thread {
            tx {
                sakLås.lås(aktørId)
                aHarLåsen.countDown()
                // Holder låsen forbi FOR UPDATE WAIT-grensen på 10 sekunder.
                aKanSlippe.await(30, TimeUnit.SECONDS)
            }
        }
        aHarLåsen.await(10, TimeUnit.SECONDS)

        val trådB = thread {
            runCatching { tx { sakLås.lås(aktørId) } }.onFailure { bFeil.set(it) }
        }
        // ~10 s WAIT + slingringsmonn: B skal gi opp av seg selv, ikke henge.
        trådB.join(25_000)

        aKanSlippe.countDown()
        trådA.join(10_000)

        assert(!trådB.isAlive) { "B skulle gitt opp etter FOR UPDATE WAIT, men venter fortsatt" }
        val feil = bFeil.get()
        assert(feil is PessimisticLockingFailureException) {
            "B skulle feilet med PessimisticLockingFailureException, men fikk: $feil"
        }
    }

    private fun tx(block: () -> Unit) {
        TransactionTemplate(transactionManager).executeWithoutResult { block() }
    }
}
