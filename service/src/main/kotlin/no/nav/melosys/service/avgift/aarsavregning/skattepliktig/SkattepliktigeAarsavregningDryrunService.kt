package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
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
    private val behandlingService: BehandlingService,
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
                if (maksAntall != null &&
                    (antallVilleOpprettetProsessinstans + antallVilleOppdatertStatus) >= maksAntall
                ) {
                    log.info { "Nådde maksAntall=$maksAntall side-effekter, stopper" }
                    return@execute
                }

                try {
                    val år = hendelse.gjelderPeriode.toIntOrNull()
                    if (år == null) {
                        log.warn { "Ugyldig gjelderPeriode: ${hendelse.gjelderPeriode} for identifikator ${hendelse.identifikator}" }
                        antallUgyldigInput++
                        return@hendelseLoop
                    }

                    val sakerMedTrygdeavgift = finnSakerMedTrygdeavgift(hendelse.identifikator, år) { fagsak, e ->
                        antallOppslagFeilet++
                        log.warn(e) { "Oppslag-feil i filter for sak ${fagsak.saksnummer}, år $år" }
                        resultater.add(
                            SakDryrunResultat(
                                saksnummer = fagsak.saksnummer,
                                gjelderAr = år,
                                identifikator = hendelse.identifikator,
                                harAktivAarsavregning = null,
                                aarsavregningBehandlingStatus = null,
                                trygdeavgiftMottaker = null,
                                villeOpprettetProsessinstans = null,
                                villeOppdatertStatus = null,
                                behandlingId = null,
                                feilmelding = e.message,
                            )
                        )
                        jobMonitor.registerException(e)
                    }

                    if (sakerMedTrygdeavgift.isEmpty()) {
                        log.debug { "Fant ingen sak med trygdeavgift for aktør: ${hendelse.identifikator}" }
                        antallUtenTreff++
                        return@hendelseLoop
                    }

                    sakerMedTrygdeavgift.forEach sakLoop@{ fagsak ->
                        antallSakerFunnet++
                        try {
                            val aktivÅrsavregning = finnAktivÅrsavregningBehandling(fagsak, år)
                            val villeOpprettetProsessinstans = aktivÅrsavregning == null
                            val villeOppdatertStatus = aktivÅrsavregning != null &&
                                aktivÅrsavregning.status != Behandlingsstatus.OPPRETTET
                            val villeHattSideEffekt = villeOpprettetProsessinstans || villeOppdatertStatus

                            if (villeHattSideEffekt &&
                                maksAntall != null &&
                                (antallVilleOpprettetProsessinstans + antallVilleOppdatertStatus) >= maksAntall
                            ) {
                                return@sakLoop
                            }

                            if (villeOpprettetProsessinstans) {
                                antallVilleOpprettetProsessinstans++
                            } else {
                                antallMedEksisterendeAarsavregning++
                            }
                            if (villeOppdatertStatus) antallVilleOppdatertStatus++

                            var prosessinstansOpprettet: Boolean? = null
                            var statusOppdatert: Boolean? = null
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
                            } else if (skarp && villeOppdatertStatus && aktivÅrsavregning != null) {
                                try {
                                    log.info {
                                        "SKARP: oppdaterer status fra ${aktivÅrsavregning.status} til VURDER_DOKUMENT for behandling ${aktivÅrsavregning.id}"
                                    }
                                    aktivÅrsavregning.status = Behandlingsstatus.VURDER_DOKUMENT
                                    behandlingService.lagre(aktivÅrsavregning)
                                    antallStatusOppdatert++
                                    statusOppdatert = true
                                } catch (e: Exception) {
                                    antallSkarpFeilet++
                                    statusOppdatert = false
                                    skarpFeilmelding = e.message
                                    log.warn(e) { "SKARP: feilet ved status-oppdatering for sak ${fagsak.saksnummer}, år $år" }
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
                                    villeOppdatertStatus = villeOppdatertStatus,
                                    behandlingId = aktivÅrsavregning?.id,
                                    prosessinstansOpprettet = prosessinstansOpprettet,
                                    statusOppdatert = statusOppdatert,
                                    feilmelding = skarpFeilmelding,
                                )
                            )
                        } catch (e: Exception) {
                            antallOppslagFeilet++
                            log.warn(e) { "Oppslag-feil for sak ${fagsak.saksnummer}, år $år" }
                            resultater.add(
                                SakDryrunResultat(
                                    saksnummer = fagsak.saksnummer,
                                    gjelderAr = år,
                                    identifikator = hendelse.identifikator,
                                    harAktivAarsavregning = null,
                                    aarsavregningBehandlingStatus = null,
                                    trygdeavgiftMottaker = null,
                                    villeOpprettetProsessinstans = null,
                                    villeOppdatertStatus = null,
                                    behandlingId = null,
                                    feilmelding = e.message,
                                )
                            )
                            jobMonitor.registerException(e)
                        }
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
                "antallVilleOppdatertStatus" to antallVilleOppdatertStatus,
                "antallOpprettet" to antallOpprettet,
                "antallStatusOppdatert" to antallStatusOppdatert,
                "antallOppslagFeilet" to antallOppslagFeilet,
                "antallSkarpFeilet" to antallSkarpFeilet,
            )
        }
    }

    private fun finnSakerMedTrygdeavgift(
        aktørId: String,
        år: Int,
        onSakFeilet: (Fagsak, Exception) -> Unit,
    ): List<Fagsak> {
        return fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter { fagsak ->
                try {
                    val relevanteBehandlinger =
                        årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, år)
                    relevanteBehandlinger?.sisteBehandlingsresultatMedAvgift
                        ?.let { behandlingsresultat -> trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) }
                        ?: false
                } catch (e: Exception) {
                    onSakFeilet(fagsak, e)
                    false
                }
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
        @Volatile var antallVilleOppdatertStatus: Int = 0,
        @Volatile var antallUtenTreff: Int = 0,
        @Volatile var antallOpprettet: Int = 0,
        @Volatile var antallStatusOppdatert: Int = 0,
        @Volatile var antallOppslagFeilet: Int = 0,
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
            antallVilleOppdatertStatus = 0
            antallUtenTreff = 0
            antallOpprettet = 0
            antallStatusOppdatert = 0
            antallOppslagFeilet = 0
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
            "antallVilleOppdatertStatus" to antallVilleOppdatertStatus,
            "antallUtenTreff" to antallUtenTreff,
            "antallOpprettet" to antallOpprettet,
            "antallStatusOppdatert" to antallStatusOppdatert,
            "antallOppslagFeilet" to antallOppslagFeilet,
            "antallSkarpFeilet" to antallSkarpFeilet,
            "result" to result,
        )
    }

    data class SakDryrunResultat(
        val saksnummer: String,
        val gjelderAr: Int,
        val identifikator: String,
        val harAktivAarsavregning: Boolean?,
        val aarsavregningBehandlingStatus: String?,
        val trygdeavgiftMottaker: String?,
        val villeOpprettetProsessinstans: Boolean?,
        val villeOppdatertStatus: Boolean?,
        val behandlingId: Long?,
        val prosessinstansOpprettet: Boolean? = null,
        val statusOppdatert: Boolean? = null,
        val feilmelding: String? = null,
    )
}

data class SkattehendelseDryrunItem(
    val gjelderPeriode: String,
    val identifikator: String
)
