package no.nav.melosys.saksflyt.metrikker

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.MeterBinder
import jakarta.annotation.PostConstruct
import no.nav.melosys.metrics.MetrikkerNavn
import no.nav.melosys.saksflytapi.domain.ProsessPrioritet
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ProsessinstansMetrikkerConfig(
    @Qualifier("saksflytThreadPoolTaskExecutor") private val saksflytThreadPoolTaskExecutor: ThreadPoolTaskExecutor
) {

    // Må holdes som felt: Micrometer holder gauge-state-objektet med en WeakReference, så et lokalt snapshot
    // ville blitt GC-et og gaugene rapportert NaN.
    private val køstørrelseSnapshot = KøstørrelseSnapshot(saksflytThreadPoolTaskExecutor.threadPoolExecutor.queue)

    @Bean
    fun prosessinstansMetrikker(statusCache: ProsessinstansStatusCache): MeterBinder = MeterBinder { registry ->
        registrerAntallFeiledeProsessinstanserGruppertPåType(registry, statusCache)
        registrerAntallFeiledeProsessinstanserGruppertPåSteg(registry, statusCache)
        registrerKøstørrelsePerPrioritet(registry)
        registrerAntallAktiveTråder(registry)
    }

    @PostConstruct
    fun init() {
        ProsessType.values().forEach { prosessType ->
            Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET, MetrikkerNavn.TAG_TYPE, prosessType.name)
        }
        ProsessSteg.values().forEach { prosessSteg ->
            listOf(ProsessStatus.FERDIG.name, ProsessStatus.FEILET.name).forEach { status ->
                Metrics.counter(
                    MetrikkerNavn.PROSESSINSTANSER_STEG_UTFØRT,
                    MetrikkerNavn.TAG_TYPE, prosessSteg.kode,
                    MetrikkerNavn.TAG_STATUS, status
                )
            }
        }
        ProsessPrioritet.values().forEach { prioritet ->
            listOf(ProsessStatus.FERDIG.name, ProsessStatus.FEILET.name).forEach { status ->
                Metrics.counter(
                    MetrikkerNavn.PROSESSINSTANSER_BEHANDLET,
                    MetrikkerNavn.TAG_PRIORITET, prioritet.name,
                    MetrikkerNavn.TAG_STATUS, status
                )
            }
        }
    }

    private fun registrerAntallFeiledeProsessinstanserGruppertPåType(
        meterRegistry: MeterRegistry,
        statusCache: ProsessinstansStatusCache
    ) {
        ProsessType.values().forEach { prosessType ->
            Gauge.builder(MetrikkerNavn.PROSESSINSTANSER_FEILET, statusCache) {
                statusCache.antallProsessinstanserFeiletPåType(prosessType)
            }
                .tag(MetrikkerNavn.TAG_PROSESSINSTANSTYPE, prosessType.kode)
                .register(meterRegistry)
        }
    }

    private fun registrerAntallFeiledeProsessinstanserGruppertPåSteg(
        meterRegistry: MeterRegistry,
        statusCache: ProsessinstansStatusCache
    ) {
        ProsessSteg.values().forEach { prosessSteg ->
            Gauge.builder(MetrikkerNavn.PROSESSINSTANSER_STEG_FEILET, statusCache) {
                statusCache.antallProsessinstanserFeiletPåSteg(prosessSteg)
            }
                .tag(MetrikkerNavn.TAG_PROSESSTEG, prosessSteg.kode)
                .register(meterRegistry)
        }
    }

    /** Antall prosessinstanser som venter i [saksflytThreadPoolTaskExecutor]-køen, brutt ned per [ProsessPrioritet]. */
    private fun registrerKøstørrelsePerPrioritet(meterRegistry: MeterRegistry) {
        ProsessPrioritet.values().forEach { prioritet ->
            Gauge.builder(MetrikkerNavn.PROSESSINSTANSER_KØ, køstørrelseSnapshot) { it.antall(prioritet).toDouble() }
                .tag(MetrikkerNavn.TAG_PRIORITET, prioritet.name)
                .register(meterRegistry)
        }
    }

    /** Antall pooltråder som akkurat nå kjører en saga (uavhengig av prioritet — en kjørende oppgave er ikke i køen). */
    private fun registrerAntallAktiveTråder(meterRegistry: MeterRegistry) {
        Gauge.builder(MetrikkerNavn.PROSESSINSTANSER_KØ_AKTIVE, saksflytThreadPoolTaskExecutor) { executor ->
            executor.activeCount.toDouble()
        }.register(meterRegistry)
    }
}
