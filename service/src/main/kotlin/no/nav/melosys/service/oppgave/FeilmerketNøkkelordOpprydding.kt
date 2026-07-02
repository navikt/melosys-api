package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveV2Client
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Rydder feilsatt nøkkelord «Årsavregning <år>» (MELOSYS-8159).
 *
 * 8128/8140 satte nøkkelordet på alle ÅRSAVREGNING-behandlinger, men EU/EØS- og trygdeavtale-yrkesaktiv
 * årsavregning blir manuelle BEH_SAK_MK-oppgaver som ikke skulle merkes. Oppgave-PATCH kan ikke fjerne
 * nøkkelord, så de feilmerkede må ryddes aktivt herfra (app-identiteten er på Oppgaves innkommende-liste).
 *
 * Selve fjerningen kan gjelde flere hundre oppgaver (full enhet-skann + GET+PATCH per oppgave), som
 * tar for lang tid for en synkron HTTP-request. Derfor kjører [ryddAsynkront] på `taskExecutor` og
 * status pollere via [status]. [finnFeilmerkede] er read-only og kan kjøres synkront som forhåndssjekk.
 */
@Service
class FeilmerketNøkkelordOpprydding(
    private val oppgaveV2Client: OppgaveV2Client
) {

    private val jobMonitor = JobMonitor(
        jobName = "FjernFeilmerketÅrsavregningNøkkelord",
        stats = OppryddingStats()
    )
    private val stats get() = jobMonitor.stats

    fun finnFeilmerkede(enhetsnr: String): NøkkelordRapport {
        val medÅrsavregningNøkkelord = mutableListOf<NøkkelordOppgave>()
        var skannet = 0
        var after: String? = null
        do {
            val respons = oppgaveV2Client.søkOppgaverForEnhet(enhetsnr, after)
            respons.path("oppgaver").values().forEach { oppgave ->
                skannet++
                klassifiser(oppgave)?.let(medÅrsavregningNøkkelord::add)
            }
            after = nesteSide(respons, after)
        } while (after != null)

        val (feilmerkede, korrektMerkede) = medÅrsavregningNøkkelord.partition { it.erFeilmerket }
        log.info {
            "Nøkkelord-opprydding enhet $enhetsnr: skannet $skannet oppgaver, " +
                "fant ${feilmerkede.size} feilmerkede og ${korrektMerkede.size} korrekt merkede"
        }
        return NøkkelordRapport(
            enhet = enhetsnr,
            antallSkannet = skannet,
            antallFeilmerkede = feilmerkede.size,
            feilmerkede = feilmerkede,
            antallKorrektMerkede = korrektMerkede.size,
            korrektMerkede = korrektMerkede
        )
    }

    /**
     * Kjører oppryddingen på en bakgrunnstråd og returnerer umiddelbart. Status leses via [status].
     * Må kalles utenfra (fra controlleren) for at @Async-proxyen skal slå inn.
     */
    @Async("taskExecutor")
    fun ryddAsynkront(enhetsnr: String, dryRun: Boolean) = runAsSystem {
        jobMonitor.execute { rydd(enhetsnr, dryRun) }
    }

    @Synchronized
    fun rydd(enhetsnr: String, dryRun: Boolean): OppryddingResultat {
        val rapport = finnFeilmerkede(enhetsnr)
        val feilmerkede = rapport.feilmerkede
        stats.antallSkannet = rapport.antallSkannet
        stats.antallFunnet = feilmerkede.size
        stats.antallKorrektMerkede = rapport.antallKorrektMerkede
        log.info {
            "Starter nøkkelord-opprydding enhet $enhetsnr: skannet ${rapport.antallSkannet}, " +
                "${feilmerkede.size} feilmerkede, ${rapport.antallKorrektMerkede} korrekt merkede, dryRun=$dryRun"
        }

        val fjernetIder = mutableListOf<String>()
        val feilet = mutableListOf<OppgaveFeil>()

        feilmerkede.forEach { oppgave ->
            if (dryRun) return@forEach
            try {
                oppgaveV2Client.fjernNøkkelord(oppgave.id) { it.matches(ÅRSAVREGNING_NØKKELORD) }
                fjernetIder.add(oppgave.id)
                stats.antallFjernet = fjernetIder.size
            } catch (e: Exception) {
                feilet.add(OppgaveFeil(oppgave.id, beskrivFeil(e)))
                stats.antallFeilet = feilet.size
                log.warn(e) { "Klarte ikke fjerne nøkkelord fra oppgave ${oppgave.id}" }
            }
        }

        log.info {
            "Fullført nøkkelord-opprydding enhet $enhetsnr: funnet=${feilmerkede.size} " +
                "fjernet=${fjernetIder.size} feilet=${feilet.size} dryRun=$dryRun"
        }
        return OppryddingResultat(
            enhet = enhetsnr,
            dryRun = dryRun,
            antallSkannet = rapport.antallSkannet,
            antallFunnet = feilmerkede.size,
            antallFjernet = fjernetIder.size,
            antallFeilet = feilet.size,
            antallKorrektMerkede = rapport.antallKorrektMerkede,
            fjernetIder = fjernetIder,
            feilet = feilet
        ).also { stats.sisteResultat = it }
    }

    // errorFilter (WebClientUtils) legger HTTP-status + responsbody inn i exception-meldingen
    // ("Kall mot Oppgave v2 feilet. 409 CONFLICT - {...}"), så meldingen forteller hvorfor PATCH-en
    // feilet: 409 = samtidig endring, 400 = validering, 429 = rate limit. Kappes for å holde /status lett.
    private fun beskrivFeil(e: Exception): String =
        (e.message ?: e.javaClass.simpleName).take(MAKS_FEILGRUNN_LENGDE)

    fun status(): Map<String, Any?> = jobMonitor.status()

    // Returnerer null for oppgaver uten årsavregning-nøkkelord; erFeilmerket = oppgavetype != BEH_ARSAVREG.
    private fun klassifiser(oppgave: JsonNode): NøkkelordOppgave? {
        val nøkkelord = oppgave.path("nokkelord").values().map { it.asString() }
        if (nøkkelord.none { it.matches(ÅRSAVREGNING_NØKKELORD) }) return null

        val kategorisering = oppgave.path("kategorisering")
        val oppgavetype = kategorisering.path("oppgavetype").path("kode").tekstEllerNull()

        return NøkkelordOppgave(
            id = oppgave.path("id").asString(),
            oppgavetype = oppgavetype,
            erFeilmerket = oppgavetype != BEH_ARSAVREG,
            nokkelord = nøkkelord,
            tema = kategorisering.path("tema").path("kode").tekstEllerNull(),
            gjelder = kategorisering.path("behandlingstema").path("term").tekstEllerNull(),
            status = oppgave.path("status").tekstEllerNull(),
            saksreferanse = oppgave.path("saksreferanse").tekstEllerNull(),
            beskrivelse = oppgave.path("beskrivelse").tekstEllerNull(),
            frist = oppgave.path("fristDato").tekstEllerNull(),
            opprettet = oppgave.path("opprettet").path("tidspunkt").tekstEllerNull()
        )
    }

    /** Rå v2-oppgave for inspeksjon av enkeltoppgaver (alle felter, inkl. tildeling/på-vent). */
    fun hentOppgave(oppgaveID: String): JsonNode = oppgaveV2Client.hentOppgaveSomJson(oppgaveID)

    // Returnerer neste sides cursor, eller null når siste side er nådd. Vakt mot et API som
    // misoppfører seg: stopp (med WARN) hvis hasNext=true men cursoren mangler eller står stille —
    // ellers ville løkka enten stoppe for tidlig eller gå evig og låse jobben (isRunning frigjøres aldri).
    private fun nesteSide(respons: JsonNode, forrigeCursor: String?): String? {
        val pagination = respons.path("pagination")
        if (!pagination.path("hasNext").asBoolean()) return null
        val nesteCursor = pagination.path("endCursor").tekstEllerNull()
        if (nesteCursor == null || nesteCursor == forrigeCursor) {
            log.warn { "Avbryter paginering: hasNext=true men endCursor=$nesteCursor (forrige=$forrigeCursor) — stopper for å unngå evig løkke / ufullstendig skann" }
            return null
        }
        return nesteCursor
    }

    private fun JsonNode.tekstEllerNull(): String? =
        if (isMissingNode || isNull) null else asString()

    // Async-tråden mangler request-konteksten, så CorrelationIdOutgoingFilter (brukt av Oppgave-klienten)
    // trenger at vi setter opp ThreadLocalAccessInfo med en system-prosess.
    private fun <T> runAsSystem(prosessSteg: String = "fjernFeilmerketNøkkelord", block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, prosessSteg)
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    inner class OppryddingStats(
        @Volatile var antallSkannet: Int = 0,
        @Volatile var antallFunnet: Int = 0,
        @Volatile var antallFjernet: Int = 0,
        @Volatile var antallFeilet: Int = 0,
        @Volatile var antallKorrektMerkede: Int = 0,
        @Volatile var sisteResultat: OppryddingResultat? = null
    ) : JobMonitor.Stats {
        override fun reset() {
            antallSkannet = 0
            antallFunnet = 0
            antallFjernet = 0
            antallFeilet = 0
            antallKorrektMerkede = 0
            sisteResultat = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "antallSkannet" to antallSkannet,
            "antallFunnet" to antallFunnet,
            "antallFjernet" to antallFjernet,
            "antallFeilet" to antallFeilet,
            "antallKorrektMerkede" to antallKorrektMerkede,
            "sisteResultat" to sisteResultat
        )
    }

    companion object {
        private const val BEH_ARSAVREG = "BEH_ARSAVREG"
        private const val MAKS_FEILGRUNN_LENGDE = 1000
        private val ÅRSAVREGNING_NØKKELORD = Regex("^Årsavregning \\d{4}$")
    }
}

// Hvorfor PATCH-en feilet for én oppgave (HTTP-status + responsbody fra Oppgave), så /status og
// GET /rapport viser årsaken direkte i stedet for at den kun havner i en WARN-logglinje.
data class OppgaveFeil(
    val id: String,
    val grunn: String
)

data class NøkkelordOppgave(
    val id: String,
    val oppgavetype: String?,
    val erFeilmerket: Boolean,
    val nokkelord: List<String>,
    val tema: String?,
    val gjelder: String?,
    val status: String?,
    val saksreferanse: String?,
    val beskrivelse: String?,
    val frist: String?,
    val opprettet: String?
)

data class NøkkelordRapport(
    val enhet: String,
    val antallSkannet: Int,
    val antallFeilmerkede: Int,
    val feilmerkede: List<NøkkelordOppgave>,
    val antallKorrektMerkede: Int,
    val korrektMerkede: List<NøkkelordOppgave>
)

// Lett resultat for /status og sisteResultat — tellere, id-liste for fjernede og id+grunn for feilede,
// ikke fulle oppgave-objekter (de kan bli mange på prod). Hent de detaljerte listene fra GET /rapport ved behov.
data class OppryddingResultat(
    val enhet: String,
    val dryRun: Boolean,
    val antallSkannet: Int,
    val antallFunnet: Int,
    val antallFjernet: Int,
    val antallFeilet: Int,
    val antallKorrektMerkede: Int,
    val fjernetIder: List<String>,
    val feilet: List<OppgaveFeil>
)
