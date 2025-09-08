package no.nav.melosys.service.ftrl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
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
    val sakerFunnet: MutableList<SakMedBehandlinger> = mutableListOf()

    private val jobMonitor = JobMonitor(
        jobName = "FinnSakerForÅrsavregningIkkeSkattepliktige",
        stats = JobStatus()
    )

    fun sakerFunnetJsonString(): String = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())
        .valueToTree<JsonNode>(sakerFunnet.map { it.toMap() }).toPrettyString()

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    fun finnSakerAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int, saksnummer: String?) {
        require(antallFeilFørStopAvJob >= 0) { "antallFeilFørStopAvJob må være positiv" }
        finnSaker(dryrun, antallFeilFørStopAvJob)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSaker(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) = runAsSystem {
        log.info { "Starter søk etter saker for årsavregning ikke-skattepliktige dryrun:$dryrun" }
        jobMonitor.execute(antallFeilFørStopAvJob) {
            finnSakerMedBehandlinger()
                .onEach { antallFunnet++ }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute
                    sakerFunnet.add(it)
                    antallProsessert++
                    if (dryrun) return@forEach
                    // TODO: bruk prosessinstansService.opprettArsavregningsBehandlingProsessflyt
                }
        }
    }

    private fun finnSakerMedBehandlinger(): List<SakMedBehandlinger> {
        val behandlinger =
            sakerForÅrsavregningRepository.finnFTRLBehandling(
                fomDato = LocalDate.of(2024, 1, 1),
                tomDato = LocalDate.of(2025, 1, 1)
            )
        return behandlinger
            .groupBy { it.fagsak }
            .map { (fagsak, behandlinger) ->
                SakMedBehandlinger(fagsak, behandlinger.sortedByDescending { it.endretDato })
            }.also { jobMonitor.stats.dbQueryStoppedAt = LocalDateTime.now() }
    }

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

    data class SakMedBehandlinger(
        val sak: Fagsak,
        val behandlinger: List<Behandling>,
    ) {
        fun toMap(): Map<String, Any?> = sak.toMap(true)

        private fun Behandling.toMap(inkluderFagsak: Boolean = true): Map<String, Any?> = mapOf(
            "id" to id,
            "status" to status.name,
            "type" to type.name,
            "tema" to tema.name,
            "registrertDato" to registrertDato,
            "endretDato" to endretDato,
            "fagsak" to if (inkluderFagsak) fagsak.toMap(inkluderBehandlinger = false) else null
        ).filterValues { it != null }

        private fun Fagsak.toMap(inkluderBehandlinger: Boolean = true) = mapOf(
            "saksnummer" to saksnummer,
            "gsakSaksnummer" to gsakSaksnummer,
            "type" to type.name,
            "tema" to tema.name,
            "status" to status.name,
            "betalingsvalg" to betalingsvalg?.name,
            "behandlinger" to if (inkluderBehandlinger) this.behandlinger.map { behandling -> behandling.toMap(false) } else null
        ).filterValues { it != null }
    }

}

interface SakerÅrsavregningIkkeSkattepliktigeRepository : CrudRepository<Behandling, Long> {
    @Query(
        """
        select distinct b
            FROM Behandlingsresultat br
            JOIN br.behandling b
            JOIN br.medlemskapsperioder mp
            JOIN br.vedtakMetadata vm
            JOIN b.fagsak f
            JOIN mp.trygdeavgiftsperioder tap
            JOIN tap.grunnlagSkatteforholdTilNorge stn
        where f.type = 'FTRL'
            and mp.fom >= :fomDato
            and mp.tom < :tomDato
            and f.status = 'LOVVALG_AVKLART'
            and stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
        """
    )
    fun finnFTRLBehandling(
        @Param("fomDato") fomDato: LocalDate,
        @Param("tomDato") tomDato: LocalDate,
    ): List<Behandling>
}
