package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import no.nav.melosys.repository.DigitalSøknadSakLockRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Integrasjonstest for DB-låsen i [DigitalSøknadSakLockRepository] (MELOSYS-8151).
 *
 * Verifiserer mot ekte Oracle at `SELECT ... FOR UPDATE` på samme aktørId serialiserer to
 * transaksjoner: tråd B får ikke låsen før tråd A committer. Ulik aktørId skal ikke blokkere.
 */
class DigitalSøknadSakLockIT(
    @Autowired private val lockRepository: DigitalSøknadSakLockRepository,
    @Autowired private val transactionManager: PlatformTransactionManager,
) : ComponentTestBase() {

    @Test
    fun `to transaksjoner på samme aktørId serialiseres til de committer`() {
        val aktørId = "laas-${UUID.randomUUID()}"
        lockRepository.sikreLåsRad(aktørId)

        val hendelser = CopyOnWriteArrayList<String>()
        val aHarLåsen = CountDownLatch(1)
        val aKanSlippe = CountDownLatch(1)

        val trådA = thread {
            tx {
                lockRepository.taRadlås(aktørId)
                hendelser.add("A-tok-lås")
                aHarLåsen.countDown()
                aKanSlippe.await()
                hendelser.add("A-committer")
            }
        }

        aHarLåsen.await(10, TimeUnit.SECONDS)

        val trådB = thread {
            tx {
                lockRepository.taRadlås(aktørId)
                hendelser.add("B-tok-lås")
            }
        }

        // Gi B tid til å forsøke å ta låsen; den skal blokkere så lenge A holder den.
        Thread.sleep(1000)
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
        lockRepository.sikreLåsRad(aktørA)
        lockRepository.sikreLåsRad(aktørB)

        val bFikkLås = CountDownLatch(1)
        val aKanSlippe = CountDownLatch(1)

        val trådA = thread {
            tx {
                lockRepository.taRadlås(aktørA)
                aKanSlippe.await()
            }
        }
        val trådB = thread {
            tx {
                lockRepository.taRadlås(aktørB)
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

    private fun tx(block: () -> Unit) {
        TransactionTemplate(transactionManager).executeWithoutResult { block() }
    }
}
