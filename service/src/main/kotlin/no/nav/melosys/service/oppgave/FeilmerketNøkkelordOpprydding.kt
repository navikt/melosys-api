package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveV2Client
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode

private val log = KotlinLogging.logger {}

/**
 * Rydder feilsatt nøkkelord «Årsavregning <år>» (MELOSYS-8159).
 *
 * 8128/8140 satte nøkkelordet på alle ÅRSAVREGNING-behandlinger, men EU/EØS- og trygdeavtale-yrkesaktiv
 * årsavregning blir manuelle BEH_SAK_MK-oppgaver som ikke skulle merkes. Oppgave-PATCH kan ikke fjerne
 * nøkkelord, så de feilmerkede må ryddes aktivt herfra (app-identiteten er på Oppgaves innkommende-liste).
 *
 * Detektor: en oppgave er feilmerket hvis et nøkkelord matcher `^Årsavregning \d{4}$` OG
 * `kategorisering.oppgavetype.kode != BEH_ARSAVREG`.
 */
@Service
class FeilmerketNøkkelordOpprydding(
    private val oppgaveV2Client: OppgaveV2Client
) {

    fun finnFeilmerkede(enhetsnr: String): List<FeilmerketOppgave> {
        val feilmerkede = mutableListOf<FeilmerketOppgave>()
        var skannet = 0
        var after: String? = null
        do {
            val respons = oppgaveV2Client.søkOppgaverForEnhet(enhetsnr, after)
            respons.path("oppgaver").values().forEach { oppgave ->
                skannet++
                tilFeilmerketOppgave(oppgave)?.let(feilmerkede::add)
            }
            after = nesteSide(respons)
        } while (after != null)
        log.info { "Nøkkelord-opprydding enhet $enhetsnr: skannet $skannet oppgaver, fant ${feilmerkede.size} feilmerkede" }
        return feilmerkede
    }

    fun rydd(enhetsnr: String, dryRun: Boolean): OppryddingResultat {
        val feilmerkede = finnFeilmerkede(enhetsnr)
        log.info { "Starter nøkkelord-opprydding enhet $enhetsnr: ${feilmerkede.size} feilmerkede, dryRun=$dryRun" }

        val fjernetIder = mutableListOf<String>()
        val feiletIder = mutableListOf<String>()
        var hoppet = 0

        feilmerkede.forEach { oppgave ->
            // Ekstra vakt: skal aldri skje gitt detektoren, men billig sikkerhet mot å røre ekte årsavregninger.
            if (oppgave.oppgavetype == BEH_ARSAVREG) {
                hoppet++
                return@forEach
            }
            if (dryRun) return@forEach
            try {
                oppgaveV2Client.fjernNøkkelord(oppgave.id) { it.matches(ÅRSAVREGNING_NØKKELORD) }
                fjernetIder.add(oppgave.id)
            } catch (e: Exception) {
                feiletIder.add(oppgave.id)
                log.warn(e) { "Klarte ikke fjerne nøkkelord fra oppgave ${oppgave.id}" }
            }
        }

        log.info {
            "Fullført nøkkelord-opprydding enhet $enhetsnr: funnet=${feilmerkede.size} " +
                "fjernet=${fjernetIder.size} feilet=${feiletIder.size} hoppet=$hoppet dryRun=$dryRun"
        }
        return OppryddingResultat(
            enhet = enhetsnr,
            dryRun = dryRun,
            antallFunnet = feilmerkede.size,
            antallFjernet = fjernetIder.size,
            antallFeilet = feiletIder.size,
            antallHoppet = hoppet,
            funnet = feilmerkede,
            fjernetIder = fjernetIder,
            feiletIder = feiletIder
        )
    }

    private fun tilFeilmerketOppgave(oppgave: JsonNode): FeilmerketOppgave? {
        val nøkkelord = oppgave.path("nokkelord").values().map { it.asString() }
        if (nøkkelord.none { it.matches(ÅRSAVREGNING_NØKKELORD) }) return null

        val kategorisering = oppgave.path("kategorisering")
        val oppgavetype = kategorisering.path("oppgavetype").path("kode").tekstEllerNull()
        if (oppgavetype == BEH_ARSAVREG) return null

        return FeilmerketOppgave(
            id = oppgave.path("id").asString(),
            oppgavetype = oppgavetype,
            nokkelord = nøkkelord,
            tema = kategorisering.path("tema").path("kode").tekstEllerNull(),
            gjelder = kategorisering.path("behandlingstema").path("term").tekstEllerNull()
        )
    }

    private fun nesteSide(respons: JsonNode): String? {
        val pagination = respons.path("pagination")
        if (!pagination.path("hasNext").asBoolean()) return null
        return pagination.path("endCursor").tekstEllerNull()
    }

    private fun JsonNode.tekstEllerNull(): String? =
        if (isMissingNode || isNull) null else asString()

    companion object {
        private const val BEH_ARSAVREG = "BEH_ARSAVREG"
        private val ÅRSAVREGNING_NØKKELORD = Regex("^Årsavregning \\d{4}$")
    }
}

data class FeilmerketOppgave(
    val id: String,
    val oppgavetype: String?,
    val nokkelord: List<String>,
    val tema: String?,
    val gjelder: String?
)

data class OppryddingResultat(
    val enhet: String,
    val dryRun: Boolean,
    val antallFunnet: Int,
    val antallFjernet: Int,
    val antallFeilet: Int,
    val antallHoppet: Int,
    val funnet: List<FeilmerketOppgave>,
    val fjernetIder: List<String>,
    val feiletIder: List<String>
)
