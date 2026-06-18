package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
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

        captureOppgave().prioritet shouldBe ProsessPrioritet.HØY
    }

    @Test
    fun `dispatch legger LAV-prioriterte batch-typer bakerst i køen`() {
        dispatcher.dispatch(Prosessinstans.forTest { type = ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING })

        captureOppgave().prioritet shouldBe ProsessPrioritet.LAV
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

    private fun captureOppgave(): PrioritertSaksflytTask {
        val oppgave = slot<Runnable>()
        verify { executor.execute(capture(oppgave)) }
        return oppgave.captured as PrioritertSaksflytTask
    }
}
