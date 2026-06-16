package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private val log = KotlinLogging.logger { }

/**
 * MELOSYS-8125 — engangs ops-jobb som sender brevet «Innhenting av inntektsopplysninger» i etterkant for
 * årsavregningsbehandlinger som allerede er opprettet for skattepliktige (skatteår 2024) via
 * MELOSYS-8045-kjøringen. De behandlingene fikk aldri brevet fordi auto-utsendingen (MELOSYS-8122)
 * ikke var live da, så vi sender det her i etterkant.
 *
 * Idempotens er rapport-drevet (ikke en egen vakt): kjør [prosesserSkattehendelser] med skarp=false
 * (dryrun) først, verifiser antallet, kjør så skarp med [maksAntall]-rampe. Ved en eventuell
 * re-kjøring fjernes allerede-sendte saker (brevSendt=true i /rapport) fra input-lista.
 *
 * Hver brev-utsending kjøres i en egen transaksjon ([InnhentingsbrevUtsender], REQUIRES_NEW), slik at
 * én sak-feil ikke ruller tilbake resten av batchen og hver sak committes inkrementelt. Brevet sendes
 * til [Mottakerroller.BRUKER]; fullmektig (FULLMEKTIG_SØKNAD) håndteres nedstrøms i brev-tjenesten,
 * slik 8122-steget også gjør.
 */
@Component
class SkattepliktigeInnhentingsbrevService(
    private val fagsakService: FagsakService,
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val innhentingsbrevUtsender: InnhentingsbrevUtsender,
) {
    val resultater: MutableList<SakInnhentingsbrevResultat> = CopyOnWriteArrayList()

    private val jobMonitor = JobMonitor(
        jobName = "SkattepliktigeInnhentingsbrev",
        stats = JobStatus()
    )

    fun rapportJsonString(): String = jacksonObjectMapper()
        .valueToTree<JsonNode>(resultater).toPrettyString()

    @Async("taskExecutor")
    @Transactional
    fun prosesserSkattehendelserAsynkront(
        skattehendelser: List<InnhentingsbrevItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) {
        prosesserSkattehendelser(skattehendelser, skarp, maksAntall)
    }

    @Synchronized
    @Transactional
    fun prosesserSkattehendelser(
        skattehendelser: List<InnhentingsbrevItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) = runAsSystem {
        resultater.clear()

        val modus = if (skarp) "SKARP" else "DRYRUN"
        log.info { "Starter $modus innhentingsbrev for ${skattehendelser.size} saker, maksAntall=$maksAntall" }

        jobMonitor.execute(maxErrorsBeforeStop = 100) {
            antallInputHendelser = skattehendelser.size
            this.skarp = skarp
            this.maksAntall = maksAntall

            // Vakt mot duplikat brev: en behandling skal aldri få innhentingsbrev mer enn én gang per
            // kjøring, uavhengig av dublikate identifikatorer i input eller personer med flere 2024-saker
            // (oppslaget er per aktør og kan returnere samme behandling fra flere input-rader).
            val sendteBehandlingIder = HashSet<Long>()

            skattehendelser.forEach hendelseLoop@{ hendelse ->
                if (jobMonitor.shouldStop) return@execute
                if (maksAntall != null && antallVilleSendtBrev >= maksAntall) {
                    log.info { "Nådde maksAntall=$maksAntall saker, stopper" }
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
                            SakInnhentingsbrevResultat(
                                saksnummer = fagsak.saksnummer,
                                gjelderAr = år,
                                identifikator = hendelse.identifikator,
                                harAktivAarsavregning = null,
                                trygdeavgiftMottaker = null,
                                behandlingId = null,
                                villeSendtBrev = null,
                                brevSendt = null,
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

                    sakerMedTrygdeavgift.forEach sakLoop@{ sak ->
                        antallSakerFunnet++
                        val fagsak = sak.fagsak
                        try {
                            val aktivÅrsavregning = finnAktivÅrsavregningBehandling(fagsak, år)

                            if (aktivÅrsavregning == null) {
                                antallUtenAarsavregning++
                                resultater.add(
                                    SakInnhentingsbrevResultat(
                                        saksnummer = fagsak.saksnummer,
                                        gjelderAr = år,
                                        identifikator = hendelse.identifikator,
                                        harAktivAarsavregning = false,
                                        trygdeavgiftMottaker = null,
                                        behandlingId = null,
                                        villeSendtBrev = false,
                                        brevSendt = null,
                                        feilmelding = null,
                                    )
                                )
                                return@sakLoop
                            }

                            if (!sendteBehandlingIder.add(aktivÅrsavregning.id)) {
                                antallDuplikatHoppetOver++
                                log.info { "Hopper over duplikat: behandling ${aktivÅrsavregning.id} (sak ${fagsak.saksnummer}) er allerede prosessert i denne kjøringen" }
                                resultater.add(
                                    SakInnhentingsbrevResultat(
                                        saksnummer = fagsak.saksnummer,
                                        gjelderAr = år,
                                        identifikator = hendelse.identifikator,
                                        harAktivAarsavregning = true,
                                        trygdeavgiftMottaker = null,
                                        behandlingId = aktivÅrsavregning.id,
                                        villeSendtBrev = false,
                                        brevSendt = null,
                                        feilmelding = "duplikat: behandling allerede prosessert i denne kjøringen",
                                    )
                                )
                                return@sakLoop
                            }

                            if (maksAntall != null && antallVilleSendtBrev >= maksAntall) {
                                return@sakLoop
                            }

                            antallVilleSendtBrev++

                            var brevSendt: Boolean? = null
                            var skarpFeilmelding: String? = null
                            if (skarp) {
                                try {
                                    innhentingsbrevUtsender.sendInnhentingsbrev(aktivÅrsavregning.id)
                                    antallBrevSendt++
                                    brevSendt = true
                                    log.info { "SKARP: sendte innhentingsbrev for sak ${fagsak.saksnummer}, behandling ${aktivÅrsavregning.id}, år $år" }
                                } catch (e: Exception) {
                                    antallBrevFeilet++
                                    brevSendt = false
                                    skarpFeilmelding = e.message
                                    log.warn(e) { "SKARP: feilet ved utsending av innhentingsbrev for sak ${fagsak.saksnummer}, behandling ${aktivÅrsavregning.id}, år $år" }
                                    jobMonitor.registerException(e)
                                }
                            }

                            val trygdeavgiftMottaker =
                                trygdeavgiftMottakerService.getTrygdeavgiftMottaker(sak.behandlingsresultatMedAvgift)

                            resultater.add(
                                SakInnhentingsbrevResultat(
                                    saksnummer = fagsak.saksnummer,
                                    gjelderAr = år,
                                    identifikator = hendelse.identifikator,
                                    harAktivAarsavregning = true,
                                    trygdeavgiftMottaker = trygdeavgiftMottaker.name,
                                    behandlingId = aktivÅrsavregning.id,
                                    villeSendtBrev = true,
                                    brevSendt = brevSendt,
                                    feilmelding = skarpFeilmelding,
                                )
                            )
                        } catch (e: Exception) {
                            antallOppslagFeilet++
                            log.warn(e) { "Oppslag-feil for sak ${fagsak.saksnummer}, år $år" }
                            resultater.add(
                                SakInnhentingsbrevResultat(
                                    saksnummer = fagsak.saksnummer,
                                    gjelderAr = år,
                                    identifikator = hendelse.identifikator,
                                    harAktivAarsavregning = null,
                                    trygdeavgiftMottaker = null,
                                    behandlingId = null,
                                    villeSendtBrev = null,
                                    brevSendt = null,
                                    feilmelding = e.message,
                                )
                            )
                            jobMonitor.registerException(e)
                        }
                    }
                } catch (e: Exception) {
                    log.warn(e) { "Feil ved prosessering av sak for identifikator ${hendelse.identifikator}" }
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
                "antallUtenAarsavregning" to antallUtenAarsavregning,
                "antallDuplikatHoppetOver" to antallDuplikatHoppetOver,
                "antallVilleSendtBrev" to antallVilleSendtBrev,
                "antallBrevSendt" to antallBrevSendt,
                "antallBrevFeilet" to antallBrevFeilet,
                "antallOppslagFeilet" to antallOppslagFeilet,
            )
        }
    }

    private fun finnSakerMedTrygdeavgift(
        aktørId: String,
        år: Int,
        onSakFeilet: (Fagsak, Exception) -> Unit,
    ): List<SakMedAvgift> {
        return fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .mapNotNull { fagsak ->
                try {
                    val behandlingsresultatMedAvgift = årsavregningService
                        .hentGjeldendeBehandlingsresultaterForÅrsavregning(fagsak.saksnummer, år)
                        ?.sisteBehandlingsresultatMedAvgift
                    if (behandlingsresultatMedAvgift != null &&
                        trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultatMedAvgift)
                    ) {
                        SakMedAvgift(fagsak, behandlingsresultatMedAvgift)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    onSakFeilet(fagsak, e)
                    null
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

    private fun <T> runAsSystem(prosessSteg: String = "skattepliktigeInnhentingsbrev", block: () -> T): T {
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
        @Volatile var antallUtenTreff: Int = 0,
        @Volatile var antallSakerFunnet: Int = 0,
        @Volatile var antallUtenAarsavregning: Int = 0,
        @Volatile var antallDuplikatHoppetOver: Int = 0,
        @Volatile var antallVilleSendtBrev: Int = 0,
        @Volatile var antallBrevSendt: Int = 0,
        @Volatile var antallBrevFeilet: Int = 0,
        @Volatile var antallOppslagFeilet: Int = 0,
        @Volatile var result: Map<String, Any?> = emptyMap(),
    ) : JobMonitor.Stats {
        override fun reset() {
            skarp = false
            maksAntall = null
            antallInputHendelser = 0
            antallUgyldigInput = 0
            antallUtenTreff = 0
            antallSakerFunnet = 0
            antallUtenAarsavregning = 0
            antallDuplikatHoppetOver = 0
            antallVilleSendtBrev = 0
            antallBrevSendt = 0
            antallBrevFeilet = 0
            antallOppslagFeilet = 0
            result = emptyMap()
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "skarp" to skarp,
            "maksAntall" to maksAntall,
            "antallInputHendelser" to antallInputHendelser,
            "antallUgyldigInput" to antallUgyldigInput,
            "antallUtenTreff" to antallUtenTreff,
            "antallSakerFunnet" to antallSakerFunnet,
            "antallUtenAarsavregning" to antallUtenAarsavregning,
            "antallDuplikatHoppetOver" to antallDuplikatHoppetOver,
            "antallVilleSendtBrev" to antallVilleSendtBrev,
            "antallBrevSendt" to antallBrevSendt,
            "antallBrevFeilet" to antallBrevFeilet,
            "antallOppslagFeilet" to antallOppslagFeilet,
            "result" to result,
        )
    }

    private data class SakMedAvgift(
        val fagsak: Fagsak,
        val behandlingsresultatMedAvgift: Behandlingsresultat,
    )

    data class SakInnhentingsbrevResultat(
        val saksnummer: String,
        val gjelderAr: Int,
        val identifikator: String,
        val harAktivAarsavregning: Boolean?,
        val trygdeavgiftMottaker: String?,
        val behandlingId: Long?,
        val villeSendtBrev: Boolean?,
        val brevSendt: Boolean? = null,
        val feilmelding: String? = null,
    )
}

/**
 * Sender selve innhentingsbrevet i en egen transaksjon (REQUIRES_NEW) slik at en feilet sak ikke
 * markerer batch-transaksjonen rollback-only, og hver sak committes inkrementelt.
 */
@Component
class InnhentingsbrevUtsender(
    private val dokumentServiceFasade: DokumentServiceFasade,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendInnhentingsbrev(behandlingId: Long) {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
        }
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto)
    }
}

data class InnhentingsbrevItem(
    val gjelderPeriode: String,
    val identifikator: String
)
