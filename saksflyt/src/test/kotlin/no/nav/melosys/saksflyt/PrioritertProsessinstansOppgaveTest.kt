package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.Prioritet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue

class PrioritertProsessinstansOppgaveTest {

    private fun nyKø() = PriorityBlockingQueue<Runnable>(16, PrioritertProsessinstansOppgave.KOMPARATOR)

    private fun oppgave(prioritet: Prioritet, registrertDato: LocalDateTime = LocalDateTime.now()) =
        PrioritertProsessinstansOppgave(UUID.randomUUID(), prioritet, registrertDato) { }

    @Test
    fun `HØY plukkes før NORMAL plukkes før LAV - uavhengig av innleggingsrekkefølge`() {
        val kø = nyKø()
        val lav = oppgave(Prioritet.LAV)
        val hoy = oppgave(Prioritet.HØY, LocalDateTime.now().plusSeconds(5)) // lagt inn senere, men HØY
        val normal = oppgave(Prioritet.NORMAL)
        kø.put(lav); kø.put(hoy); kø.put(normal)

        (kø.take() as PrioritertProsessinstansOppgave).prioritet shouldBe Prioritet.HØY
        (kø.take() as PrioritertProsessinstansOppgave).prioritet shouldBe Prioritet.NORMAL
        (kø.take() as PrioritertProsessinstansOppgave).prioritet shouldBe Prioritet.LAV
    }

    @Test
    fun `FIFO innen samme prioritet - eldste registrertDato først`() {
        val kø = nyKø()
        val eldst = oppgave(Prioritet.LAV, LocalDateTime.now().minusMinutes(10))
        val nyest = oppgave(Prioritet.LAV, LocalDateTime.now())
        kø.put(nyest); kø.put(eldst)

        kø.take() shouldBe eldst
        kø.take() shouldBe nyest
    }

    @Test
    fun `en HØY-oppgave lagt inn etter mange LAV kjøres før de gjenværende LAV`() {
        val kø = nyKø()
        repeat(50) { kø.put(oppgave(Prioritet.LAV)) }
        val hoy = oppgave(Prioritet.HØY)
        kø.put(hoy)

        kø.take() shouldBe hoy
    }

    @Test
    fun `ukjent Runnable behandles som NORMAL`() {
        val kø = nyKø()
        val lav = oppgave(Prioritet.LAV)
        val hoy = oppgave(Prioritet.HØY)
        val ukjent = Runnable { }
        kø.put(lav); kø.put(ukjent); kø.put(hoy)

        kø.take() shouldBe hoy    // HØY først
        kø.take() shouldBe ukjent // NORMAL (ukjent) før LAV
        kø.take() shouldBe lav
    }

    @Test
    fun `run delegerer til den underliggende oppgaven`() {
        var kjørt = false
        PrioritertProsessinstansOppgave(UUID.randomUUID(), Prioritet.NORMAL, LocalDateTime.now()) { kjørt = true }.run()
        kjørt shouldBe true
    }

    @Test
    fun `run svelger uventet feil slik at pooltråden ikke dør`() {
        // Skal ikke kaste videre
        PrioritertProsessinstansOppgave(UUID.randomUUID(), Prioritet.NORMAL, LocalDateTime.now()) {
            throw RuntimeException("uventet")
        }.run()
    }
}
