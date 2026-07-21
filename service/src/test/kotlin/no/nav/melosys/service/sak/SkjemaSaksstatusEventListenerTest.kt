package no.nav.melosys.service.sak

import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakStatusEndretEvent
import no.nav.melosys.domain.forTest
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class SkjemaSaksstatusEventListenerTest {

    @MockK
    lateinit var skjemaSakMappingRepository: SkjemaSakMappingRepository

    @MockK
    lateinit var prosessinstansService: ProsessinstansService

    private lateinit var listener: SkjemaSaksstatusEventListener

    @BeforeEach
    fun setup() {
        listener = SkjemaSaksstatusEventListener(skjemaSakMappingRepository, prosessinstansService)
    }

    @Test
    fun `bestiller synk-prosessinstans for sak med skjema-mapping`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(fagsak.saksnummer) } returns true
        every { prosessinstansService.opprettProsessinstansSynkSkjemaSaksstatus(fagsak.saksnummer) } just runs

        listener.fagsakStatusEndret(FagsakStatusEndretEvent(fagsak))

        verify(exactly = 1) { prosessinstansService.opprettProsessinstansSynkSkjemaSaksstatus(fagsak.saksnummer) }
    }

    @Test
    fun `bestiller ikke synk for sak uten skjema-mapping`() {
        val fagsak = Fagsak.forTest()
        every { skjemaSakMappingRepository.existsByFagsak_Saksnummer(fagsak.saksnummer) } returns false

        listener.fagsakStatusEndret(FagsakStatusEndretEvent(fagsak))

        verify(exactly = 0) { prosessinstansService.opprettProsessinstansSynkSkjemaSaksstatus(any()) }
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
