package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue

class PrioritertSaksflytTaskTest {

    private fun nyKø() = PriorityBlockingQueue<Runnable>(16, PrioritertSaksflytTask.KOMPARATOR)

    private fun oppgave(prioritet: ProsessPrioritet, registrertDato: LocalDateTime = LocalDateTime.now()) =
        PrioritertSaksflytTask(UUID.randomUUID(), prioritet, registrertDato) { }

    @Test
    fun `HØY plukkes før NORMAL plukkes før LAV - uavhengig av innleggingsrekkefølge`() {
        val kø = nyKø()
        val lav = oppgave(ProsessPrioritet.LAV)
        val høy = oppgave(ProsessPrioritet.HØY, LocalDateTime.now().plusSeconds(5)) // lagt inn senere, men HØY
        val normal = oppgave(ProsessPrioritet.NORMAL)
        kø.put(lav); kø.put(høy); kø.put(normal)

        (kø.take() as PrioritertSaksflytTask).prioritet shouldBe ProsessPrioritet.HØY
        (kø.take() as PrioritertSaksflytTask).prioritet shouldBe ProsessPrioritet.NORMAL
        (kø.take() as PrioritertSaksflytTask).prioritet shouldBe ProsessPrioritet.LAV
    }

    @Test
    fun `FIFO innen samme prioritet - eldste registrertDato først`() {
        val kø = nyKø()
        val eldst = oppgave(ProsessPrioritet.LAV, LocalDateTime.now().minusMinutes(10))
        val nyest = oppgave(ProsessPrioritet.LAV, LocalDateTime.now())
        kø.put(nyest); kø.put(eldst)

        kø.take() shouldBe eldst
        kø.take() shouldBe nyest
    }

    @Test
    fun `en HØY-oppgave lagt inn etter mange LAV kjøres før de gjenværende LAV`() {
        val kø = nyKø()
        repeat(50) { kø.put(oppgave(ProsessPrioritet.LAV)) }
        val høy = oppgave(ProsessPrioritet.HØY)
        kø.put(høy)

        kø.take() shouldBe høy
    }

    @Test
    fun `ukjent Runnable behandles som NORMAL`() {
        val kø = nyKø()
        val lav = oppgave(ProsessPrioritet.LAV)
        val høy = oppgave(ProsessPrioritet.HØY)
        val ukjent = Runnable { }
        kø.put(lav); kø.put(ukjent); kø.put(høy)

        kø.take() shouldBe høy    // HØY først
        kø.take() shouldBe ukjent // NORMAL (ukjent) før LAV
        kø.take() shouldBe lav
    }

    @Test
    fun `ukjent Runnable sorteres bakerst i NORMAL-båndet - snyter seg ikke foran legitimt NORMAL-arbeid`() {
        val kø = nyKø()
        val normal = oppgave(ProsessPrioritet.NORMAL, LocalDateTime.now()) // tidligere registrertDato enn MAX-fallback for ukjente
        val ukjent = Runnable { }
        kø.put(ukjent); kø.put(normal)

        kø.take() shouldBe normal
        kø.take() shouldBe ukjent
    }

    @Test
    fun `run delegerer til den underliggende oppgaven`() {
        var kjørt = false
        PrioritertSaksflytTask(UUID.randomUUID(), ProsessPrioritet.NORMAL, LocalDateTime.now()) { kjørt = true }.run()
        kjørt shouldBe true
    }

    @Test
    fun `run svelger uventet feil slik at pooltråden ikke dør`() {
        // Skal ikke kaste videre
        PrioritertSaksflytTask(UUID.randomUUID(), ProsessPrioritet.NORMAL, LocalDateTime.now()) {
            throw RuntimeException("uventet")
        }.run()
    }
}
