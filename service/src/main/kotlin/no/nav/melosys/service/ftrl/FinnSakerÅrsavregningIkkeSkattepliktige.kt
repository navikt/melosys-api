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
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

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
    fun finnSakerAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int, saksnummer: String?, fomDato: LocalDate, tomDato: LocalDate) {
        require(antallFeilFørStopAvJob >= 0) { "antallFeilFørStopAvJob må være 0 eller positiv" }
        finnSaker(dryrun, antallFeilFørStopAvJob, fomDato, tomDato)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSaker(
        dryrun: Boolean,
        antallFeilFørStopAvJob: Int = 0,
        fomDato: LocalDate,
        tomDato: LocalDate
    ) = runAsSystem {
        log.info {
            "Starter søk etter saker for årsavregning ikke-skattepliktige " +
                "\n dryrun: $dryrun" +
                "\n medlemskapsperiode fom: $fomDato" +
                "\n medlemskapsperiode tom: $tomDato"
        }
        jobMonitor.execute(antallFeilFørStopAvJob) {
            finnSakerMedBehandlinger(fomDato, tomDato)
                .onEach { antallFunnet++ }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute
                    sakerFunnet.add(it)
                    antallProsessert++
                    if (dryrun) return@forEach
                    // TODO: bruk prosessinstansService.opprettArsavregningsBehandlingProsessflyt
                }
            result = sakerFunnet
                .associate { it.sak.saksnummer to it.behandlinger.size }
                .toList()
                .sortedByDescending { it.second }
                .toMap()
        }
    }

    private fun finnSakerMedBehandlinger(fomDato: LocalDate, tomDato: LocalDate): List<SakMedBehandlinger> =
        BehandlingshistorikkCache(fomDato = fomDato, tomDato = tomDato).finnSakerMedBehandlinger()

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
        @Volatile var result: Map<String, Any?> = emptyMap(),
        @Volatile var finnFTRLBehandlingerdbQueryStoppedAt: LocalDateTime? = null,
        @Volatile var finnSakerMedTidligereÅrsavregningQueryStoppedAt: LocalDateTime? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            antallFunnet = 0
            antallProsessert = 0
            finnFTRLBehandlingerdbQueryStoppedAt = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "dbQueryRuntime-finnFTRLBehandlinger" to jobMonitor.durationUntil(finnFTRLBehandlingerdbQueryStoppedAt),
            "dbQueryRuntime-finnSakerMedTidligereÅrsavregning" to jobMonitor.durationUntil(finnSakerMedTidligereÅrsavregningQueryStoppedAt),
            "antallFunnet" to antallFunnet,
            "antallProsessert" to antallProsessert,
            "result" to result,
        )
    }

    data class SakMedBehandlinger(
        val sak: Fagsak,
        val behandlinger: List<Behandling>,
    ) {
        fun toMap(): Map<String, Any?> = sak.toMap(behandlinger)

        private fun Behandling.toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "status" to status.name,
            "type" to type.name,
            "tema" to tema.name,
            "registrertDato" to registrertDato,
            "endretDato" to endretDato,
        ).filterValues { it != null }

        private fun Fagsak.toMap(behandlinger: List<Behandling>?) = mapOf(
            "saksnummer" to saksnummer,
            "gsakSaksnummer" to gsakSaksnummer,
            "type" to type.name,
            "tema" to tema.name,
            "status" to status.name,
            "betalingsvalg" to betalingsvalg?.name,
            "behandlinger" to behandlinger?.map { behandling -> behandling.toMap() }
        ).filterValues { it != null }
    }

    inner class BehandlingshistorikkCache(private val fomDato: LocalDate, private val tomDato: LocalDate) {
        private val sakerMedTidligereFastsettingCache: Map<String, List<Behandling>> by lazy {
            sakerMedTidligereFastsetting().also {
                log.info { "Fant ${it.size} saker med tidligere årsavregning med fastsetting" }
            }
        }

        fun finnSakerMedBehandlinger(): List<SakMedBehandlinger> =
            sakerForÅrsavregningRepository.finnFTRLBehandlinger(fomDato = fomDato, tomDato = tomDato)
                .filterNot {
                    sakerMedTidligereFastsettingCache[it.fagsak.saksnummer]?.let { behandlinger ->
                        log.info { "Ekskluderer sak ${it.fagsak.saksnummer} pga ${behandlinger.size}) tidligere årsavregning med fastsetting" }
                        true
                    } ?: false
                }
                .groupBy { it.fagsak }
                .map { (fagsak, behandlinger) ->
                    SakMedBehandlinger(fagsak, behandlinger.sortedByDescending { it.endretDato })
                }.also { jobMonitor.stats.finnFTRLBehandlingerdbQueryStoppedAt = LocalDateTime.now() }

        private fun sakerMedTidligereFastsetting(): Map<String, List<Behandling>> =
            sakerForÅrsavregningRepository
                .finnSakerMedTidligereÅrsavregningOgFastsetting(
                    fomDato = fomDato
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                    tomDato = tomDato
                        .atTime(LocalTime.MAX)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
                .groupBy { it.fagsak.saksnummer }
                .mapValues { it.value.sortedByDescending { b -> b.endretDato } }
                .also {
                    jobMonitor.stats.finnSakerMedTidligereÅrsavregningQueryStoppedAt = LocalDateTime.now()
                }
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
        WHERE f.type = 'FTRL'
            and f.status = 'LOVVALG_AVKLART'
            and mp.fom >= :fomDato
            and mp.tom <= :tomDato
            and EXISTS (
                SELECT 1 FROM mp.trygdeavgiftsperioder tap
                JOIN tap.grunnlagSkatteforholdTilNorge stn
                WHERE stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
            )
            """
    )
    fun finnFTRLBehandlinger(
        @Param("fomDato") fomDato: LocalDate,
        @Param("tomDato") tomDato: LocalDate,
    ): List<Behandling>

    @Query(
        """
        select distinct b
        FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.type = 'FTRL'
            and b.type = 'ÅRSAVREGNING'
            and br.type = 'FASTSATT_TRYGDEAVGIFT'
            and br.registrertDato between :fomDato and :tomDato
        """
    )
    fun finnSakerMedTidligereÅrsavregningOgFastsetting(
        @Param("fomDato") fomDato: java.time.Instant,
        @Param("tomDato") tomDato: java.time.Instant,
    ): List<Behandling>
}
