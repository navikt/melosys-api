package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

@Component
class SkattepliktigeAarsavregningDryrunService(
    private val fagsakService: FagsakService,
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {
    val resultater: MutableList<SakDryrunResultat> = mutableListOf()

    private val jobMonitor = JobMonitor(
        jobName = "SkattepliktigeAarsavregningDryrun",
        stats = JobStatus()
    )

    fun rapportJsonString(): String = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())
        .valueToTree<JsonNode>(resultater).toPrettyString()

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    fun prosesserSkattehendelserAsynkront(skattehendelser: List<SkattehendelseDryrunItem>) {
        prosesserSkattehendelser(skattehendelser)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun prosesserSkattehendelser(skattehendelser: List<SkattehendelseDryrunItem>) = runAsSystem {
        resultater.clear()

        log.info { "Starter dryrun for ${skattehendelser.size} skattehendelser" }

        jobMonitor.execute {
            antallInputHendelser = skattehendelser.size

            skattehendelser.forEach { hendelse ->
                if (jobMonitor.shouldStop) return@execute

                val år = hendelse.gjelderPeriode.toIntOrNull()
                if (år == null) {
                    log.warn { "Ugyldig gjelderPeriode: ${hendelse.gjelderPeriode} for identifikator ${hendelse.identifikator}" }
                    antallUgyldigInput++
                    return@forEach
                }

                val sakerMedTrygdeavgift = finnSakerMedTrygdeavgift(hendelse.identifikator, år)

                if (sakerMedTrygdeavgift.isEmpty()) {
                    log.debug { "Fant ingen sak med trygdeavgift for aktør: ${hendelse.identifikator}" }
                    antallUtenTreff++
                    return@forEach
                }

                sakerMedTrygdeavgift.forEach { fagsak ->
                    antallSakerFunnet++
                    val aktivÅrsavregning = finnAktivÅrsavregningBehandling(fagsak, år)
                    val villeOpprettetProsessinstans = aktivÅrsavregning == null

                    if (villeOpprettetProsessinstans) {
                        antallVilleOpprettetProsessinstans++
                    } else {
                        antallMedEksisterendeAarsavregning++
                    }

                    val behandlingsresultat = årsavregningService
                        .hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, år)
                        ?.sisteBehandlingsresultatMedAvgift

                    val trygdeavgiftMottaker = behandlingsresultat?.let {
                        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(it)
                    }

                    resultater.add(
                        SakDryrunResultat(
                            saksnummer = fagsak.saksnummer,
                            gjelderAr = år,
                            identifikator = hendelse.identifikator,
                            harAktivAarsavregning = aktivÅrsavregning != null,
                            aarsavregningBehandlingStatus = aktivÅrsavregning?.status?.name,
                            trygdeavgiftMottaker = trygdeavgiftMottaker?.name,
                            villeOpprettetProsessinstans = villeOpprettetProsessinstans,
                            behandlingId = aktivÅrsavregning?.id
                        )
                    )
                }
            }

            result = mapOf(
                "antallInputHendelser" to antallInputHendelser,
                "antallUgyldigInput" to antallUgyldigInput,
                "antallUtenTreff" to antallUtenTreff,
                "antallSakerFunnet" to antallSakerFunnet,
                "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
                "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning
            )
        }
    }

    private fun finnSakerMedTrygdeavgift(aktørId: String, år: Int): List<Fagsak> {
        return fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter {
                val relevanteBehandlinger =
                    årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(it.saksnummer, år)
                relevanteBehandlinger?.sisteBehandlingsresultatMedAvgift
                    ?.let { behandlingsresultat -> trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) }
                    ?: false
            }
    }

    private fun finnAktivÅrsavregningBehandling(fagsak: Fagsak, gjelderÅr: Int): Behandling? {
        val årsAvregninger = fagsak.hentAktiveÅrsavregninger()
            .filter { behandlingsresultatService.hentBehandlingsresultat(it.id).hentÅrsavregning().aar == gjelderÅr }

        return when {
            årsAvregninger.isEmpty() -> null
            årsAvregninger.size > 1 -> {
                log.warn { "Flere aktive årsavregninger funnet for sak: ${fagsak.saksnummer} og år: $gjelderÅr" }
                årsAvregninger.first()
            }
            else -> årsAvregninger.single()
        }
    }

    private fun <T> runAsSystem(prosessSteg: String = "skattepliktigeAarsavregningDryrun", block: () -> T): T {
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
        @Volatile var antallInputHendelser: Int = 0,
        @Volatile var antallUgyldigInput: Int = 0,
        @Volatile var antallSakerFunnet: Int = 0,
        @Volatile var antallVilleOpprettetProsessinstans: Int = 0,
        @Volatile var antallMedEksisterendeAarsavregning: Int = 0,
        @Volatile var antallUtenTreff: Int = 0,
        @Volatile var result: Map<String, Any?> = emptyMap(),
        @Volatile var dbQueryStoppedAt: LocalDateTime? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            antallInputHendelser = 0
            antallUgyldigInput = 0
            antallSakerFunnet = 0
            antallVilleOpprettetProsessinstans = 0
            antallMedEksisterendeAarsavregning = 0
            antallUtenTreff = 0
            dbQueryStoppedAt = null
            result = emptyMap()
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "dbQueryRuntime" to jobMonitor.durationUntil(dbQueryStoppedAt),
            "antallInputHendelser" to antallInputHendelser,
            "antallUgyldigInput" to antallUgyldigInput,
            "antallSakerFunnet" to antallSakerFunnet,
            "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
            "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning,
            "antallUtenTreff" to antallUtenTreff,
            "result" to result,
        )
    }

    data class SakDryrunResultat(
        val saksnummer: String,
        val gjelderAr: Int,
        val identifikator: String,
        val harAktivAarsavregning: Boolean,
        val aarsavregningBehandlingStatus: String?,
        val trygdeavgiftMottaker: String?,
        val villeOpprettetProsessinstans: Boolean,
        val behandlingId: Long?
    )
}

data class SkattehendelseDryrunItem(
    val gjelderPeriode: String,
    val identifikator: String
)
