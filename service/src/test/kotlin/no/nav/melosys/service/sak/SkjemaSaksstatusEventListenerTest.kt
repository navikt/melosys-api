package no.nav.melosys.service.sak

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakStatusEndretEvent
import no.nav.melosys.domain.forTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

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
    fun `delegerer til sync-servicen med fagsaken fra eventet`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) } just runs

        listener.fagsakStatusEndret(FagsakStatusEndretEvent(fagsak))

        verify(exactly = 1) { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) }
    }

    @Test
    fun `feil fra synkronisering kastes ikke videre og velter ikke saksbehandlingsflyten`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSaksstatusSyncService.synkroniserSaksstatusForFagsak(fagsak) } throws
            RuntimeException("skjema-api er nede")

        assertDoesNotThrow {
            listener.fagsakStatusEndret(FagsakStatusEndretEvent(fagsak))
        }
    }

    @Test
    fun `lytter etter commit slik at rollback ikke kan gi feil status i skjema-api og feil ikke ruller tilbake statusendringen`() {
        val annotasjon = SkjemaSaksstatusEventListener::class.java
            .getMethod("fagsakStatusEndret", FagsakStatusEndretEvent::class.java)
            .getAnnotation(TransactionalEventListener::class.java)

        annotasjon.shouldNotBeNull()
        annotasjon.phase shouldBe TransactionPhase.AFTER_COMMIT
        annotasjon.fallbackExecution shouldBe true
    }
}
