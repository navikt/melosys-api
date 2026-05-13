package no.nav.melosys.saksflyt.metrikker

import no.nav.melosys.saksflyt.PrioritertSaksflytTask
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import java.util.EnumMap
import java.util.Queue

/**
 * Cacher antall [Runnable]-er i [kø] per [ProsessPrioritet] i [gyldighetMs] ms, slik at én Prometheus-scrape kun
 * itererer over køen én gang i stedet for én gang per prioritet.
 *
 * Brukeren ([ProsessinstansMetrikkerConfig]) må holde instansen som et felt — Micrometer holder gauge-state-objektet
 * med en `WeakReference`, så et lokalt snapshot ville blitt GC-et og gaugene rapportert `NaN`.
 */
internal class KøstørrelseSnapshot(
    private val kø: Queue<Runnable>,
    private val gyldighetMs: Long = 1_000L,
    private val nåMs: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var harBeregnet = false
    @Volatile private var beregnetTidspunktMs = 0L
    @Volatile private var antallPerPrioritet: Map<ProsessPrioritet, Int> = emptyMap()

    fun antall(prioritet: ProsessPrioritet): Int {
        oppfriskVedBehov()
        return antallPerPrioritet[prioritet] ?: 0
    }

    @Synchronized
    private fun oppfriskVedBehov() {
        val nå = nåMs()
        if (harBeregnet && nå - beregnetTidspunktMs < gyldighetMs) return
        val telling = EnumMap<ProsessPrioritet, Int>(ProsessPrioritet::class.java)
        kø.forEach { telling.merge(PrioritertSaksflytTask.prioritetAv(it), 1, Int::plus) }
        antallPerPrioritet = telling
        beregnetTidspunktMs = nå
        harBeregnet = true
    }
}
