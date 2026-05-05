package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.saksflytapi.ProsessinstansService
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
    private val prosessinstansService: ProsessinstansService,
) {
    val resultater: MutableList<SakDryrunResultat> = mutableListOf()

    private val jobMonitor = JobMonitor(
        jobName = "SkattepliktigeAarsavregningDryrun",
        stats = JobStatus()
    )

    fun rapportJsonString(): String = jacksonObjectMapper()
        .valueToTree<JsonNode>(resultater).toPrettyString()

    @Async("taskExecutor")
    @Transactional
    fun prosesserSkattehendelserAsynkront(
        skattehendelser: List<SkattehendelseDryrunItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) {
        prosesserSkattehendelser(skattehendelser, skarp, maksAntall)
    }

    @Synchronized
    @Transactional
    fun prosesserSkattehendelser(
        skattehendelser: List<SkattehendelseDryrunItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) = runAsSystem {
        resultater.clear()

        val modus = if (skarp) "SKARP" else "DRYRUN"
        log.info { "Starter $modus for ${skattehendelser.size} skattehendelser, maksAntall=$maksAntall" }

        jobMonitor.execute(maxErrorsBeforeStop = 100) {
            antallInputHendelser = skattehendelser.size
            this.skarp = skarp
            this.maksAntall = maksAntall

            skattehendelser.forEach hendelseLoop@{ hendelse ->
                if (jobMonitor.shouldStop) return@execute
                if (maksAntall != null && antallVilleOpprettetProsessinstans >= maksAntall) {
                    log.info { "Nådde maksAntall=$maksAntall, stopper" }
                    return@execute
                }

                try {
                    val år = hendelse.gjelderPeriode.toIntOrNull()
                    if (år == null) {
                        log.warn { "Ugyldig gjelderPeriode: ${hendelse.gjelderPeriode} for identifikator ${hendelse.identifikator}" }
                        antallUgyldigInput++
                        return@hendelseLoop
                    }

                    val sakerMedTrygdeavgift = finnSakerMedTrygdeavgift(hendelse.identifikator, år)

                    if (sakerMedTrygdeavgift.isEmpty()) {
                        log.debug { "Fant ingen sak med trygdeavgift for aktør: ${hendelse.identifikator}" }
                        antallUtenTreff++
                        return@hendelseLoop
                    }

                    sakerMedTrygdeavgift.forEach sakLoop@{ fagsak ->
                        antallSakerFunnet++
                        val aktivÅrsavregning = finnAktivÅrsavregningBehandling(fagsak, år)
                        val villeOpprettetProsessinstans = aktivÅrsavregning == null

                        if (villeOpprettetProsessinstans &&
                            maksAntall != null && antallVilleOpprettetProsessinstans >= maksAntall
                        ) {
                            return@sakLoop
                        }

                        if (villeOpprettetProsessinstans) {
                            antallVilleOpprettetProsessinstans++
                        } else {
                            antallMedEksisterendeAarsavregning++
                        }

                        var prosessinstansOpprettet: Boolean? = null
                        var skarpFeilmelding: String? = null
                        if (skarp && villeOpprettetProsessinstans) {
                            try {
                                prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                                    fagsak.saksnummer,
                                    hendelse.gjelderPeriode,
                                    Behandlingsaarsaktyper.MELDING_FRA_SKATT,
                                )
                                antallOpprettet++
                                prosessinstansOpprettet = true
                                log.info { "SKARP: opprettet årsavregning-prosessinstans for sak ${fagsak.saksnummer}, år $år" }
                            } catch (e: Exception) {
                                antallSkarpFeilet++
                                prosessinstansOpprettet = false
                                skarpFeilmelding = e.message
                                log.warn(e) { "SKARP: feilet ved opprettelse for sak ${fagsak.saksnummer}, år $år" }
                                jobMonitor.registerException(e)
                            }
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
                                behandlingId = aktivÅrsavregning?.id,
                                prosessinstansOpprettet = prosessinstansOpprettet,
                                feilmelding = skarpFeilmelding,
                            )
                        )
                    }
                } catch (e: Exception) {
                    log.warn(e) { "Feil ved prosessering av hendelse for identifikator ${hendelse.identifikator}" }
                    jobMonitor.registerException(e)
                }
            }

            result = mapOf(
                "modus" to modus,
                "skarp" to skarp,
                "maksAntall" to maksAntall,
                "antallInputHendelser" to antallInputHendelser,
                "antallUgyldigInput" to antallUgyldigInput,
                "antallUtenTreff" to antallUtenTreff,
                "antallSakerFunnet" to antallSakerFunnet,
                "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
                "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning,
                "antallOpprettet" to antallOpprettet,
                "antallSkarpFeilet" to antallSkarpFeilet,
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
        @Volatile var skarp: Boolean = false,
        @Volatile var maksAntall: Int? = null,
        @Volatile var antallInputHendelser: Int = 0,
        @Volatile var antallUgyldigInput: Int = 0,
        @Volatile var antallSakerFunnet: Int = 0,
        @Volatile var antallVilleOpprettetProsessinstans: Int = 0,
        @Volatile var antallMedEksisterendeAarsavregning: Int = 0,
        @Volatile var antallUtenTreff: Int = 0,
        @Volatile var antallOpprettet: Int = 0,
        @Volatile var antallSkarpFeilet: Int = 0,
        @Volatile var result: Map<String, Any?> = emptyMap(),
        @Volatile var dbQueryStoppedAt: LocalDateTime? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            skarp = false
            maksAntall = null
            antallInputHendelser = 0
            antallUgyldigInput = 0
            antallSakerFunnet = 0
            antallVilleOpprettetProsessinstans = 0
            antallMedEksisterendeAarsavregning = 0
            antallUtenTreff = 0
            antallOpprettet = 0
            antallSkarpFeilet = 0
            dbQueryStoppedAt = null
            result = emptyMap()
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "skarp" to skarp,
            "maksAntall" to maksAntall,
            "dbQueryRuntime" to jobMonitor.durationUntil(dbQueryStoppedAt),
            "antallInputHendelser" to antallInputHendelser,
            "antallUgyldigInput" to antallUgyldigInput,
            "antallSakerFunnet" to antallSakerFunnet,
            "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
            "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning,
            "antallUtenTreff" to antallUtenTreff,
            "antallOpprettet" to antallOpprettet,
            "antallSkarpFeilet" to antallSkarpFeilet,
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
        val behandlingId: Long?,
        val prosessinstansOpprettet: Boolean? = null,
        val feilmelding: String? = null,
    )
}

data class SkattehendelseDryrunItem(
    val gjelderPeriode: String,
    val identifikator: String
)
