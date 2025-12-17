package no.nav.melosys.service.kodeverk

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.context.event.ApplicationReadyEvent

@ExtendWith(MockKExtension::class)
class KodeverkSchedulerConfigTest {

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var applicationReadyEvent: ApplicationReadyEvent

    private lateinit var kodeverkSchedulerConfig: KodeverkSchedulerConfig

    @BeforeEach
    fun setup() {
        kodeverkSchedulerConfig = KodeverkSchedulerConfig(kodeverkService)
        every { kodeverkService.lastKodeverk() } returns Unit
    }

    @Test
    fun `onApplicationReady skal kalle lastKodeverk`() {
        kodeverkSchedulerConfig.onApplicationReady(applicationReadyEvent)

        verify(exactly = 1) { kodeverkService.lastKodeverk() }
    }

    @Test
    fun `scheduledKodeverkRefresh skal kalle lastKodeverk`() {
        kodeverkSchedulerConfig.scheduledKodeverkRefresh()

        verify(exactly = 1) { kodeverkService.lastKodeverk() }
    }
}
