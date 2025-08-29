package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class FinnSakerÅrsavregningIkkeSkattepliktige(
    private val sakerForÅrsavregningRepository: SakerÅrsavregningIkkeSkattepliktigeRepository
) {
    private val jobMonitor = JobMonitor(
        jobName = "FinnSakerForÅrsavregningIkkeSkattepliktige",
        stats = JobStatus()
    )

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    fun finnSakerAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int, saksnummer: String?) {
        finnSaker(dryrun, antallFeilFørStopAvJob)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSaker(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) = runAsSystem {
        jobMonitor.execute(antallFeilFørStopAvJob) {
            finnSakerHvorÅrsavregningSkalOpprettes()
                .onEach { antallFunnet++ }
                .filterNot { dryrun }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute
                    println(it.saksnummer)
                    antallProsessert++
                }
        }
    }

    private fun finnSakerHvorÅrsavregningSkalOpprettes(): List<Fagsak> =
        sakerForÅrsavregningRepository.finnFTRLFagsaker(
            sakStatuser = listOf(
                Saksstatuser.LOVVALG_AVKLART,
                Saksstatuser.AVSLUTTET,
                Saksstatuser.MEDLEMSKAP_AVKLART
            ),
            behandlingsStatuser = listOf(
                Behandlingsstatus.AVSLUTTET,
                Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            ),
            ekskluderteBehandlingsresultater = listOf(
                Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            ),
            fomDato = LocalDate.of(2024, 1, 1)
        ).also { jobMonitor.stats.dbQueryStoppedAt = LocalDateTime.now() }

    private fun <T> runAsSystem(prosessSteg: String = "finnSakerHvorÅrsavregningSkalOpprettes", block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, prosessSteg)
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    fun status() = jobMonitor.status()

    inner class JobStatus(
        @Volatile var antallFunnet: Int = 0,
        @Volatile var antallProsessert: Int = 0,
        @Volatile var dbQueryStoppedAt: LocalDateTime? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            antallFunnet = 0
            antallProsessert = 0
            dbQueryStoppedAt = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "dbQueryRuntime" to jobMonitor.durationUntil(dbQueryStoppedAt),
            "antallFunnet" to antallFunnet,
            "antallProsessert" to antallProsessert,
        )
    }
}

interface SakerÅrsavregningIkkeSkattepliktigeRepository : CrudRepository<Fagsak, Long> {
    @Query(
        """
        select distinct f
            FROM Behandlingsresultat br
            JOIN br.behandling b
            JOIN br.medlemskapsperioder mp
            JOIN br.vedtakMetadata vm
            JOIN b.fagsak f
            JOIN mp.trygdeavgiftsperioder tap
            JOIN tap.grunnlagSkatteforholdTilNorge stn
        where f.type = 'FTRL'
            and f.status in :sakStatuser
            and b.status in :behandlingsStatuser
            and br.type not in :ekskluderteBehandlingsresultater
            and mp.fom >= :fomDato
            and stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
        """
    )
    fun finnFTRLFagsaker(
        @Param("sakStatuser") sakStatuser: List<Saksstatuser>,
        @Param("behandlingsStatuser") behandlingsStatuser: List<Behandlingsstatus>,
        @Param("ekskluderteBehandlingsresultater") ekskluderteBehandlingsresultater: List<Behandlingsresultattyper>,
        @Param("fomDato") fomDato: LocalDate,
    ): List<Fagsak>
}
