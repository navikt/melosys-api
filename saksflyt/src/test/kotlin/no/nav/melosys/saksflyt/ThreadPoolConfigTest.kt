package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.concurrent.PriorityBlockingQueue

class ThreadPoolConfigTest {

    @Test
    fun `saksflytThreadPoolTaskExecutor er en prioritetskø-executor med 3 arbeidertråder`() {
        val executor = ThreadPoolConfig().saksflytThreadPoolTaskExecutor()
        try {
            executor.shouldBeInstanceOf<PrioritertSaksflytTaskExecutor>()
            executor.corePoolSize shouldBe 3
            // corePoolSize=3 + ubegrenset (PriorityBlockingQueue) kø => alltid nøyaktig 3 arbeidertråder.
            executor.threadPoolExecutor.queue.shouldBeInstanceOf<PriorityBlockingQueue<Runnable>>()
        } finally {
            executor.shutdown()
        }
    }
}
