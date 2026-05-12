package no.nav.melosys.saksflyt.metrikker

import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflyt.PrioritertProsessinstansOppgave
import no.nav.melosys.saksflytapi.domain.Prioritet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue

class KøstørrelseSnapshotTest {

    private fun oppgave(prioritet: Prioritet) =
        PrioritertProsessinstansOppgave(UUID.randomUUID(), prioritet, LocalDateTime.now()) { }

    private fun nyKø() = PriorityBlockingQueue<Runnable>(16, PrioritertProsessinstansOppgave.KOMPARATOR)

    @Test
    fun `teller køelementene gruppert per prioritet - ukjent Runnable telles som NORMAL`() {
        val kø = nyKø().apply {
            put(oppgave(Prioritet.HØY))
            put(oppgave(Prioritet.LAV))
            put(oppgave(Prioritet.LAV))
            put(Runnable { })
        }

        val snapshot = KøstørrelseSnapshot(kø)

        snapshot.antall(Prioritet.HØY) shouldBe 1
        snapshot.antall(Prioritet.NORMAL) shouldBe 1
        snapshot.antall(Prioritet.LAV) shouldBe 2
    }

    @Test
    fun `cacher innenfor gyldighetsvinduet og friskner opp etterpå`() {
        val kø = nyKø().apply { put(oppgave(Prioritet.LAV)) }
        var nå = 1_000L
        val snapshot = KøstørrelseSnapshot(kø, gyldighetMs = 100L, nåMs = { nå })

        snapshot.antall(Prioritet.LAV) shouldBe 1

        kø.put(oppgave(Prioritet.LAV)) // endring i køen skal ikke ses så lenge cachen er gyldig
        nå = 1_050L
        snapshot.antall(Prioritet.LAV) shouldBe 1

        nå = 1_200L // forbi gyldighetsvinduet -> ny telling
        snapshot.antall(Prioritet.LAV) shouldBe 2
    }
}
