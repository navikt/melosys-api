package no.nav.melosys.saksflyt.metrikker

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflyt.PrioritertSaksflytTask
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue

class KøstørrelseSnapshotTest {

    private fun oppgave(prioritet: ProsessPrioritet) =
        PrioritertSaksflytTask(UUID.randomUUID(), prioritet, LocalDateTime.now()) { }

    private fun nyKø() = PriorityBlockingQueue<Runnable>(16, PrioritertSaksflytTask.KOMPARATOR)

    @Test
    fun `teller køelementene gruppert per prioritet - ukjent Runnable telles som NORMAL`() {
        val kø = nyKø().apply {
            put(oppgave(ProsessPrioritet.HØY))
            put(oppgave(ProsessPrioritet.LAV))
            put(oppgave(ProsessPrioritet.LAV))
            put(Runnable { })
        }

        val snapshot = KøstørrelseSnapshot(kø)

        snapshot.antall(ProsessPrioritet.HØY) shouldBe 1
        snapshot.antall(ProsessPrioritet.NORMAL) shouldBe 1
        snapshot.antall(ProsessPrioritet.LAV) shouldBe 2
    }

    @Test
    fun `cacher innenfor gyldighetsvinduet og friskner opp etterpå`() {
        val kø = nyKø().apply { put(oppgave(ProsessPrioritet.LAV)) }
        var nå = 1_000L
        val snapshot = KøstørrelseSnapshot(kø, gyldighetMs = 100L, nåMs = { nå })

        snapshot.antall(ProsessPrioritet.LAV) shouldBe 1

        kø.put(oppgave(ProsessPrioritet.LAV)) // endring i køen skal ikke ses så lenge cachen er gyldig
        nå = 1_050L
        snapshot.antall(ProsessPrioritet.LAV) shouldBe 1

        nå = 1_200L // forbi gyldighetsvinduet -> ny telling
        snapshot.antall(ProsessPrioritet.LAV) shouldBe 2
    }
}
