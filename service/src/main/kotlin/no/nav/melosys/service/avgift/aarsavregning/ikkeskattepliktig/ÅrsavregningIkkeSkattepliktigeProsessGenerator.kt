package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

@Component
class ÅrsavregningIkkeSkattepliktigeProsessGenerator(
    private val årsavregningIkkeSkattepliktigeFinner: ÅrsavregningIkkeSkattepliktigeFinner,
    private val prosessinstansService: ProsessinstansService
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
        require(fomDato.year == tomDato.year) { "fomDato og tomDato må være i samme år. fomDato: $fomDato, tomDato: $tomDato" }
        finnSaker(dryrun, antallFeilFørStopAvJob, fomDato, tomDato, saksnummer)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSaker(
        dryrun: Boolean,
        antallFeilFørStopAvJob: Int = 0,
        fomDato: LocalDate,
        tomDato: LocalDate,
        saksnummer: String? = null,
    ) = runAsSystem {
        require(fomDato.year == tomDato.year) { "fomDato og tomDato må være i samme år. fomDato: $fomDato, tomDato: $tomDato" }

        val år = fomDato.year.toString()

        log.info {
            "Starter søk etter saker for årsavregning ikke-skattepliktige " +
                "\n dryrun: $dryrun" +
                "\n år: $år" +
                "\n medlemskapsperiode fom: $fomDato" +
                "\n medlemskapsperiode tom: $tomDato"
        }

        jobMonitor.execute(antallFeilFørStopAvJob) {
            finnSakerMedBehandlinger(fomDato, tomDato)
                .filter { saksnummer == null || it.sak.saksnummer == saksnummer }
                .onEach { antallFunnet++ }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute
                    sakerFunnet.add(it)
                    antallProsessert++
                    if (dryrun) return@forEach
                    prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                        it.sak.saksnummer,
                        år,
                        Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
                    )
                }
            result = sakerFunnet
                .associate { it.sak.saksnummer to it.behandlinger.size }
                .toList()
                .sortedByDescending { it.second }
                .toMap()
        }
    }

    private fun finnSakerMedBehandlinger(fomDato: LocalDate, tomDato: LocalDate): List<SakMedBehandlinger> =
        årsavregningIkkeSkattepliktigeFinner.finnSakerMedBehandlinger(fomDato = fomDato, tomDato = tomDato) {
            // Callback for å oppdatere når DB-spørringen er ferdig
            jobMonitor.stats.finnSakerMedTidligereÅrsavregningQueryStoppedAt = LocalDateTime.now()
        }.also {
            jobMonitor.stats.finnFTRLBehandlingerdbQueryStoppedAt = LocalDateTime.now()
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
}
