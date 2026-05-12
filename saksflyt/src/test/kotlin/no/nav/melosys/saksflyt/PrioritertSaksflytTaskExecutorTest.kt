package no.nav.melosys.saksflyt

import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.concurrent.PriorityBlockingQueue

class PrioritertSaksflytTaskExecutorTest {

    @Test
    fun `bruker en PriorityBlockingQueue etter initialize`() {
        val executor = PrioritertSaksflytTaskExecutor().apply {
            corePoolSize = 1
            setWaitForTasksToCompleteOnShutdown(false)
            initialize()
        }
        try {
            executor.threadPoolExecutor.queue.shouldBeInstanceOf<PriorityBlockingQueue<Runnable>>()
        } finally {
            executor.shutdown()
        }
    }
}
