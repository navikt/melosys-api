package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.Prioritet
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class ProsessinstansDispatcherTest {

    private val executor = mockk<ThreadPoolTaskExecutor>(relaxed = true)
    private val prosessinstansBehandler = mockk<ProsessinstansBehandler>(relaxed = true)
    private val dispatcher = ProsessinstansDispatcher(executor, prosessinstansBehandler)

    @Test
    fun `dispatch legger oppgave på køen med ProsessType sin default-prioritet`() {
        dispatcher.dispatch(Prosessinstans.forTest { type = ProsessType.IVERKSETT_VEDTAK_EOS })

        captureOppgave().prioritet shouldBe Prioritet.HØY
    }

    @Test
    fun `dispatch bruker per-kall-overstyring lagret på prosessinstansen`() {
        dispatcher.dispatch(Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING // default NORMAL
            medData(ProsessDataKey.PRIORITET, Prioritet.LAV)
        })

        captureOppgave().prioritet shouldBe Prioritet.LAV
    }

    @Test
    fun `oppgavens run kaller behandleProsessinstansNå`() {
        val prosessinstans = Prosessinstans.forTest { }
        val oppgave = slot<Runnable>()
        every { executor.execute(capture(oppgave)) } just Runs

        dispatcher.dispatch(prosessinstans)
        oppgave.captured.run()

        verify { prosessinstansBehandler.behandleProsessinstansNå(prosessinstans) }
    }

    private fun captureOppgave(): PrioritertProsessinstansOppgave {
        val oppgave = slot<Runnable>()
        verify { executor.execute(capture(oppgave)) }
        return oppgave.captured as PrioritertProsessinstansOppgave
    }
}
