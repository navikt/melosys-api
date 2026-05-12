package no.nav.melosys.saksflyt.metrikker

import no.nav.melosys.saksflyt.PrioritertProsessinstansOppgave
import no.nav.melosys.saksflytapi.domain.Prioritet
import java.util.EnumMap
import java.util.Queue

/**
 * Teller [Runnable]-ene i [kø] gruppert per [Prioritet] i ett løp og cacher resultatet i [gyldighetMs] ms.
 *
 * Én Prometheus-scrape leser alle `melosys.prosessinstanser.ko{prioritet=…}`-gaugene rett etter hverandre, så
 * cachen gjør at det blir kun én weakly-consistent O(n)-iterasjon over køen per scrape i stedet for én per [Prioritet].
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
    @Volatile private var antallPerPrioritet: Map<Prioritet, Int> = emptyMap()

    fun antall(prioritet: Prioritet): Int {
        oppfriskVedBehov()
        return antallPerPrioritet[prioritet] ?: 0
    }

    @Synchronized
    private fun oppfriskVedBehov() {
        val nå = nåMs()
        if (harBeregnet && nå - beregnetTidspunktMs < gyldighetMs) return
        val telling = EnumMap<Prioritet, Int>(Prioritet::class.java)
        kø.forEach { telling.merge(PrioritertProsessinstansOppgave.prioritetAv(it), 1, Int::plus) }
        antallPerPrioritet = telling
        beregnetTidspunktMs = nå
        harBeregnet = true
    }
}
