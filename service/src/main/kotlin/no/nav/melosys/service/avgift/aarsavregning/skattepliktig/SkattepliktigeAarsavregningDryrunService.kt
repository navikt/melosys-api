package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.SkattepliktigAarsavregningOpprettelseService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.Collections
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Kjører de samme vurderingene som Kafka-flyten, men i batch mot en liste med skattehendelser, og
 * legger en rapport rundt dem. Med `skarp = false` er kjøringen en simulering.
 *
 * Vurderingene selv ligger i [SkattepliktigAarsavregningOpprettelseService], som Kafka-flyten
 * bruker, slik at de to svarer likt på samme sak. Det som er verktøyets eget er rapporten, taket på
 * antall side-effekter, dedupliseringen og per sak-feilhåndteringen: consumeren lar et kast
 * propagere til Kafka-retry, mens en batch må notere feilen og gå videre.
 */
@Component
class SkattepliktigeAarsavregningDryrunService(
    private val opprettelseService: SkattepliktigAarsavregningOpprettelseService,
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val skarpUtfoerer: SkattepliktigeAarsavregningSkarpUtfoerer,
) {
    // Skrives fra @Async-tråden mens /rapport kan lese samtidig.
    val resultater: MutableList<SakDryrunResultat> = Collections.synchronizedList(mutableListOf())

    private val jobMonitor = JobMonitor(
        jobName = "SkattepliktigeAarsavregningDryrun",
        stats = JobStatus()
    )

    fun rapportJsonString(): String {
        // synchronizedList synkroniserer enkeltoperasjoner, ikke traversering. Jackson indekserer
        // seg gjennom lista, så et samtidig resultater.clear() — som skjer når en ny kjøring startes
        // mens rapporten hentes — gir IndexOutOfBoundsException midt i serialiseringen. Derfor
        // snapshot under låsen; monitoren er wrapper-objektet selv, jf. Javadoc for synchronizedList.
        val snapshot = synchronized(resultater) { ArrayList(resultater) }
        return jacksonObjectMapper().valueToTree<JsonNode>(snapshot).toPrettyString()
    }

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    fun prosesserSkattehendelserAsynkront(
        skattehendelser: List<SkattehendelseDryrunItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) {
        prosesserSkattehendelser(skattehendelser, skarp, maksAntall)
    }

    // readOnly: alle skrivninger går gjennom skarpUtfoerer i egne transaksjoner, så denne stien skal
    // være garantert bivirkningsfri — readOnly gir FlushMode.MANUAL og er garantien. NB: kalles som
    // selvkall fra metoden over, så denne annotasjonen gjelder kun direkte kallere (tester).
    // Garantien for controller-stien ligger på den ytre metoden og er pinnet av
    // SkattepliktigeAarsavregningSkarpIT.
    @Transactional(readOnly = true)
    fun prosesserSkattehendelser(
        skattehendelser: List<SkattehendelseDryrunItem>,
        skarp: Boolean = false,
        maksAntall: Int? = null,
    ) = runAsSystem {
        val modus = if (skarp) "SKARP" else "DRYRUN"
        log.info { "Starter $modus for ${skattehendelser.size} skattehendelser, maksAntall=$maksAntall" }

        // Vakten mot to samtidige kjøringer er jobMonitor.isRunning, som avviser den andre i stedet
        // for å køe den. En @Synchronized her ville i stedet holdt den andre kjøringen ventende og så
        // kjørt den skarpt etterpå, mot en base den første nettopp endret.
        jobMonitor.execute(maxErrorsBeforeStop = 100) {
            // Etter execute: en avvist kjøring skal ikke slette rapporten fra den som kjører.
            resultater.clear()
            antallInputHendelser = skattehendelser.size
            this.skarp = skarp
            this.maksAntall = maksAntall

            // To hendelser for samme person og år ville gitt to prosessinstanser: opprettelsen er
            // ikke idempotent på sak/år — saga-steget revaliderer ikke før det oppretter
            // behandlingen, og en prosessinstans som bare ligger i kø er usynlig for
            // finnAktivÅrsavregningBehandling. Resultatet er to årsavregninger og to
            // innhentingsbrev til samme borger. Input er ekte skattehendelser, og en korrigert
            // skattemelding gir en ny hendelse for et år vi allerede har sett, så duplikater er
            // forventet.
            //
            // Dette lukker duplikater innenfor én kjøring. Overlappende kjøringer — typisk en
            // canary etterfulgt av full kjøring før køen er tømt — er det samme hullet og må
            // håndteres i prosedyren: vent til de køede instansene er ferdige, og hold
            // canary-sakene utenfor neste kjøring. Kafka-flyten har nøyaktig samme hull; den varige
            // fiksen hører hjemme i saga-steget og er egen oppgave.
            // Året parses før dedupliseringen: input bygges for hånd fra en SQL-dump, så «2023» og
            // «02023» forekommer om hverandre og er samme år. Dedupliserer vi på den rå strengen,
            // slipper de gjennom som to hendelser.
            val (gyldige, ugyldige) = skattehendelser
                .map { it to it.gjelderPeriode.toIntOrNull() }
                .partition { (_, år) -> år != null }
            antallUgyldigInput = ugyldige.size
            ugyldige.forEach { (hendelse, _) ->
                log.warn { "Ugyldig gjelderPeriode: ${hendelse.gjelderPeriode} for identifikator ${hendelse.identifikator}" }
            }

            val unikeHendelser = gyldige.map { (hendelse, år) -> NormalisertHendelse(hendelse.identifikator, år!!) }.distinct()
            antallDuplikaterFjernet = gyldige.size - unikeHendelser.size
            if (antallDuplikaterFjernet > 0) {
                log.warn {
                    "Fjernet $antallDuplikaterFjernet duplikate hendelser (samme identifikator og år) " +
                        "av ${gyldige.size} gyldige — de ville gitt doble årsavregninger og doble brev"
                }
            }

            fun taketErFylt() = maksAntall != null &&
                (antallVilleOpprettetProsessinstans + antallVilleOppdatertStatus) >= maksAntall

            // Egen blokk rundt løkka slik at et avbrudd forlater løkka, ikke hele jobben:
            // oppsummeringen under skal bygges også når kjøringen stoppes halvveis.
            run hendelser@{
                unikeHendelser.forEach hendelseLoop@{ hendelse ->
                    if (jobMonitor.shouldStop) return@hendelser
                    if (taketErFylt()) {
                        log.info { "Nådde maksAntall=$maksAntall side-effekter, stopper" }
                        stoppetPgaTak = true
                        return@hendelser
                    }
                    antallHendelserProsessert++

                    val år = hendelse.år
                    try {
                        var sakerFeiletIFilter = 0
                        val sakerMedTrygdeavgift =
                            opprettelseService.finnSakerMedTrygdeavgift(hendelse.identifikator, år) { fagsak, e ->
                                sakerFeiletIFilter++
                                antallSakerIkkeVurdert++
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
                            // Bare når alle sakene faktisk ble vurdert. Feilet noen, vet vi ikke om
                            // aktøren har en sak med trygdeavgift, og «uten treff» ville sagt at
                            // aktøren er avklart.
                            if (sakerFeiletIFilter == 0) {
                                log.debug { "Fant ingen sak med trygdeavgift for aktør: ${hendelse.identifikator}" }
                                antallUtenTreff++
                            }
                            return@hendelseLoop
                        }

                        sakerMedTrygdeavgift.forEach sakLoop@{ fagsak ->
                            if (jobMonitor.shouldStop) return@hendelser
                            antallSakerFunnet++
                            try {
                                val aktivÅrsavregning = opprettelseService.finnAktivÅrsavregningBehandling(fagsak, år)
                                val villeOpprettetProsessinstans = aktivÅrsavregning == null
                                val villeOppdatertStatus = aktivÅrsavregning != null &&
                                    aktivÅrsavregning.status != Behandlingsstatus.OPPRETTET
                                val villeHattSideEffekt = villeOpprettetProsessinstans || villeOppdatertStatus

                                if (villeHattSideEffekt && taketErFylt()) {
                                    // Saken er talt i antallSakerFunnet, så den må også havne i en
                                    // kategori og i rapporten — ellers finnes den ingen steder for
                                    // den som kjører canary med et lavt tak.
                                    antallSakerHoppetOverPgaTak++
                                    stoppetPgaTak = true
                                    resultater.add(
                                        SakDryrunResultat(
                                            saksnummer = fagsak.saksnummer,
                                            gjelderAr = år,
                                            identifikator = hendelse.identifikator,
                                            harAktivAarsavregning = aktivÅrsavregning != null,
                                            aarsavregningBehandlingStatus = aktivÅrsavregning?.status?.name,
                                            trygdeavgiftMottaker = null,
                                            villeOpprettetProsessinstans = null,
                                            villeOppdatertStatus = null,
                                            behandlingId = aktivÅrsavregning?.id,
                                            hoppetOverAarsak = "nådde maksAntall=$maksAntall før saken ble vurdert",
                                        )
                                    )
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
                                var hoppetOverAarsak: String? = null
                                var skarpFeilmelding: String? = null
                                if (skarp && villeOpprettetProsessinstans) {
                                    try {
                                        skarpUtfoerer.opprettProsessinstans(
                                            fagsak.saksnummer,
                                            år.toString(),
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
                                        val bump = skarpUtfoerer.settStatusVurderDokument(
                                            aktivÅrsavregning.id,
                                            aktivÅrsavregning.status,
                                        )
                                        statusOppdatert = bump.oppdatert
                                        if (bump.oppdatert) {
                                            antallStatusOppdatert++
                                        } else {
                                            antallStatusHoppetOver++
                                            hoppetOverAarsak = "status var ${bump.faktiskStatus} ved skriving, " +
                                                "løkka observerte ${aktivÅrsavregning.status}"
                                        }
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
                                        hoppetOverAarsak = hoppetOverAarsak,
                                        feilmelding = skarpFeilmelding,
                                    )
                                )
                            } catch (e: Exception) {
                                antallSakerFeilet++
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
            }

            result = mapOf(
                "modus" to modus,
                "skarp" to skarp,
                "maksAntall" to maksAntall,
                "antallInputHendelser" to antallInputHendelser,
                "antallDuplikaterFjernet" to antallDuplikaterFjernet,
                "antallUgyldigInput" to antallUgyldigInput,
                "antallUtenTreff" to antallUtenTreff,
                "antallSakerFunnet" to antallSakerFunnet,
                "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
                "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning,
                "antallVilleOppdatertStatus" to antallVilleOppdatertStatus,
                "antallOpprettet" to antallOpprettet,
                "antallStatusOppdatert" to antallStatusOppdatert,
                "antallStatusHoppetOver" to antallStatusHoppetOver,
                "antallSakerIkkeVurdert" to antallSakerIkkeVurdert,
                "antallSakerFeilet" to antallSakerFeilet,
                "antallSakerHoppetOverPgaTak" to antallSakerHoppetOverPgaTak,
                "antallHendelserProsessert" to antallHendelserProsessert,
                "stoppetPgaTak" to stoppetPgaTak,
                "antallSkarpFeilet" to antallSkarpFeilet,
            )
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

    /**
     * Jobbtilstanden lever i minnet på én pod, og appen kjører to replikaer. Går /run til pod A og
     * /status til pod B, ser den siste en tom kjøring — og `@Synchronized` på
     * [prosesserSkattehendelser] er per pod, så den hindrer heller ikke to samtidige skarpe
     * kjøringer. Riktig medisin er å kjøre mot én pod (port-forward); `pod` er her for at den som
     * kjører skal kunne se hvilken pod svaret kommer fra.
     */
    fun status() = jobMonitor.status() + mapOf("pod" to (System.getenv("HOSTNAME") ?: "ukjent"))

    inner class JobStatus(
        @Volatile var skarp: Boolean = false,
        @Volatile var maksAntall: Int? = null,
        @Volatile var antallInputHendelser: Int = 0,
        @Volatile var antallDuplikaterFjernet: Int = 0,
        @Volatile var antallUgyldigInput: Int = 0,
        /**
         * Saker som passerte filteret. Deles mellom [antallVilleOpprettetProsessinstans],
         * [antallMedEksisterendeAarsavregning], [antallSakerFeilet] og
         * [antallSakerHoppetOverPgaTak] — de fire summerer til denne.
         */
        @Volatile var antallSakerFunnet: Int = 0,
        /** Saker uten aktiv årsavregning for året. */
        @Volatile var antallVilleOpprettetProsessinstans: Int = 0,
        /** Saker med aktiv årsavregning for året. */
        @Volatile var antallMedEksisterendeAarsavregning: Int = 0,
        /**
         * Delmengde av [antallMedEksisterendeAarsavregning]: de der statusen ikke er OPPRETTET og
         * derfor skal oppdateres. Ikke en egen kategori — ikke legg den til de andre.
         */
        @Volatile var antallVilleOppdatertStatus: Int = 0,
        @Volatile var antallUtenTreff: Int = 0,
        @Volatile var antallOpprettet: Int = 0,
        @Volatile var antallStatusOppdatert: Int = 0,
        @Volatile var antallStatusHoppetOver: Int = 0,
        /** Saker som feilet før vi visste om de har trygdeavgift — de er ikke med i [antallSakerFunnet]. */
        @Volatile var antallSakerIkkeVurdert: Int = 0,
        /** Saker som feilet under vurderingen — de er med i [antallSakerFunnet]. */
        @Volatile var antallSakerFeilet: Int = 0,
        /** Saker som ble nådd etter at taket var fylt, og derfor ikke vurdert — med i [antallSakerFunnet]. */
        @Volatile var antallSakerHoppetOverPgaTak: Int = 0,
        /** Hvor mange av hendelsene som faktisk ble kjørt. Lavere enn [antallInputHendelser] hvis kjøringen ble avbrutt. */
        @Volatile var antallHendelserProsessert: Int = 0,
        /** Satt når taket avbrøt kjøringen, så resten av lista ikke er gjort. */
        @Volatile var stoppetPgaTak: Boolean = false,
        @Volatile var antallSkarpFeilet: Int = 0,
        @Volatile var result: Map<String, Any?> = emptyMap(),
    ) : JobMonitor.Stats {
        override fun reset() {
            skarp = false
            maksAntall = null
            antallInputHendelser = 0
            antallDuplikaterFjernet = 0
            antallUgyldigInput = 0
            antallSakerFunnet = 0
            antallVilleOpprettetProsessinstans = 0
            antallMedEksisterendeAarsavregning = 0
            antallVilleOppdatertStatus = 0
            antallUtenTreff = 0
            antallOpprettet = 0
            antallStatusOppdatert = 0
            antallStatusHoppetOver = 0
            antallSakerIkkeVurdert = 0
            antallSakerFeilet = 0
            antallSakerHoppetOverPgaTak = 0
            antallHendelserProsessert = 0
            stoppetPgaTak = false
            antallSkarpFeilet = 0
            result = emptyMap()
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "skarp" to skarp,
            "maksAntall" to maksAntall,
            "antallInputHendelser" to antallInputHendelser,
            "antallDuplikaterFjernet" to antallDuplikaterFjernet,
            "antallUgyldigInput" to antallUgyldigInput,
            "antallSakerFunnet" to antallSakerFunnet,
            "antallVilleOpprettetProsessinstans" to antallVilleOpprettetProsessinstans,
            "antallMedEksisterendeAarsavregning" to antallMedEksisterendeAarsavregning,
            "antallVilleOppdatertStatus" to antallVilleOppdatertStatus,
            "antallUtenTreff" to antallUtenTreff,
            "antallOpprettet" to antallOpprettet,
            "antallStatusOppdatert" to antallStatusOppdatert,
            "antallStatusHoppetOver" to antallStatusHoppetOver,
            "antallSakerIkkeVurdert" to antallSakerIkkeVurdert,
            "antallSakerFeilet" to antallSakerFeilet,
            "antallSakerHoppetOverPgaTak" to antallSakerHoppetOverPgaTak,
            "antallHendelserProsessert" to antallHendelserProsessert,
            "stoppetPgaTak" to stoppetPgaTak,
            "antallSkarpFeilet" to antallSkarpFeilet,
            "result" to result,
        )
    }

    data class SakDryrunResultat(
        val saksnummer: String,
        val gjelderAr: Int,
        val identifikator: String,
        val harAktivAarsavregning: Boolean?,
        /** Status slik den ble observert før en eventuell skarp statusoppdatering; se [statusOppdatert]. */
        val aarsavregningBehandlingStatus: String?,
        val trygdeavgiftMottaker: String?,
        val villeOpprettetProsessinstans: Boolean?,
        val villeOppdatertStatus: Boolean?,
        val behandlingId: Long?,
        val prosessinstansOpprettet: Boolean? = null,
        val statusOppdatert: Boolean? = null,
        /** Satt når statusoppdateringen ble hoppet over fordi raden hadde endret seg — skiller det fra [feilmelding]. */
        val hoppetOverAarsak: String? = null,
        val feilmelding: String? = null,
    )
}

/** Hendelsen etter at året er parset, slik at «2023» og «02023» er samme nøkkel. */
private data class NormalisertHendelse(val identifikator: String, val år: Int)

data class SkattehendelseDryrunItem(
    val gjelderPeriode: String,
    val identifikator: String
)
