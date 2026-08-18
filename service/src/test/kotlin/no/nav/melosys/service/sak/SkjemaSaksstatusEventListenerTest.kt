package no.nav.melosys.service.sak

import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.FagsakStatusEndretEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional

@ExtendWith(MockKExtension::class)
internal class SkjemaSaksstatusEventListenerTest {

    @MockK
    lateinit var skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService

    private lateinit var listener: SkjemaSaksstatusEventListener

    @BeforeEach
    fun setup() {
        listener = SkjemaSaksstatusEventListener(skjemaSaksstatusSyncService)
    }

    @Test
    fun `delegerer til bestillSynkHvisSkjemakoblet med eventets saksnummer`() {
        // Skjemakoblet-guarden og selve bestillingen testes i SkjemaSaksstatusSyncServiceTest
        every { skjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet("MEL-100") } just runs

        listener.fagsakStatusEndret(FagsakStatusEndretEvent("MEL-100"))

        verify(exactly = 1) { skjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet("MEL-100") }
    }

    @Test
    fun `bestillingen skjer transaksjonelt i samme transaksjon som statusendringen`() {
        // Outbox-semantikk: @EventListener (synkront, i pågående transaksjon) + @Transactional —
        // bestillingen committes atomisk med statusendringen og kan ikke tapes ved krasj.
        val metode = SkjemaSaksstatusEventListener::class.java
            .getMethod("fagsakStatusEndret", FagsakStatusEndretEvent::class.java)

        metode.getAnnotation(EventListener::class.java).shouldNotBeNull()
        metode.getAnnotation(Transactional::class.java).shouldNotBeNull()
    }
}
