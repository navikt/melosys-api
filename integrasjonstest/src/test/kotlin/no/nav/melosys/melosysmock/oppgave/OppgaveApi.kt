package no.nav.melosys.melosysmock.oppgave

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/v1/oppgaver")
@Unprotected
class OppgaveApi(private val oppgaveRepo: OppgaveRepo) {

    @GetMapping
    fun hentOppgaveliste(
        @RequestParam("aktoerId", required = false) aktoerId: String? = null,
        @RequestParam("orgnr", required = false) orgnr: String? = null,
        @RequestParam("tildeltEnhetsnr", required = false) tildeltEnhetsnr: String? = null,
        @RequestParam("tildeltRessurs", required = false) tildeltRessurs: Boolean? = null,
        @RequestParam("tilordnetRessurs", required = false) tilordnetRessurs: String? = null,
        @RequestParam("statuskategori", required = false) statuskategori: String? = null,
        @RequestParam("behandlesAvApplikasjon", required = false) behandlesAvApplikasjon: String? = null,
        @RequestParam("behandlingstype", required = false) behandlingstype: String? = null,
        @RequestParam("behandlingstema", required = false) behandlingstema: String? = null,
        @RequestParam("saksreferanse", required = false) saksreferanse: String? = null
    ): OppgavelisteRespons {
        val oppgaver = oppgaveRepo.repo.values
            .filter {
                (tildeltEnhetsnr == null || tildeltEnhetsnr == it.tildeltEnhetsnr) &&
                    (tildeltRessurs == null || !tildeltRessurs && it.tilordnetRessurs == null || tildeltRessurs && it.tilordnetRessurs != null) &&
                    (tilordnetRessurs == null || tilordnetRessurs == it.tilordnetRessurs) &&
                    (statuskategori == null || harStatuskategori(statuskategori, it)) &&
                    (behandlesAvApplikasjon == null || behandlesAvApplikasjon == it.behandlesAvApplikasjon) &&
                    (behandlingstype == null || behandlingstype == it.behandlingstype) &&
                    (behandlingstema == null || behandlingstema == it.behandlingstema) &&
                    (saksreferanse == null || saksreferanse == it.saksreferanse)
            }
            .filter {
                (orgnr == null && aktoerId == null) ||
                    (orgnr != null && orgnr == it.orgnr) ||
                    (aktoerId != null && aktoerId == it.aktoerId)
            }

        return OppgavelisteRespons(oppgaver.size, oppgaver)
    }

    private fun harStatuskategori(statuskategori: String, oppgave: Oppgave) = when (statuskategori) {
        "AAPEN" -> oppgave.status != "FERDIGSTILT"
        else -> true
    }

    @GetMapping("/{oppgaveID}")
    fun hentOppgave(@PathVariable("oppgaveID") oppgaveID: Int): ResponseEntity<Oppgave> {
        return if (oppgaveRepo.repo.containsKey(oppgaveID)) ResponseEntity.ok(oppgaveRepo.repo[oppgaveID]!!)
        else ResponseEntity.notFound().build()
    }

    @PutMapping("/{oppgaveID}")
    fun oppdaterOppgave(
        @PathVariable("oppgaveID") oppgaveID: Int,
        @RequestBody oppgave: Oppgave
    ): ResponseEntity<Oppgave> {
        if (!oppgaveRepo.repo.containsKey(oppgaveID)) return ResponseEntity.notFound().build()

        oppgaveRepo.repo[oppgaveID] = oppgave
        oppgave.id = oppgaveID
        oppgave.versjon++
        oppgave.endretTidspunkt = LocalDateTime.now()
        return ResponseEntity.ok(oppgave)
    }

    @PostMapping
    fun opprettOppgave(@RequestBody oppgave: Oppgave): ResponseEntity<Oppgave> {
        val oppgaveID = oppgaveRepo.finnSisteOppgaveId() + 1
        oppgave.id = oppgaveID
        oppgave.versjon = 1
        oppgaveRepo.repo[oppgaveID] = oppgave
        oppgave.endretTidspunkt = LocalDateTime.now()
        oppgave.opprettetTidspunkt = ZonedDateTime.now()
        return ResponseEntity.ok(oppgave)
    }
}


data class Oppgave(
    var id: Int? = null,
    val aktivDato: LocalDate? = LocalDate.now(),
    val aktoerId: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val beskrivelse: String? = null,
    val bnr: String? = null,
    val endretAv: String? = null,
    val endretAvEnhetsnr: String? = null,
    var endretTidspunkt: LocalDateTime? = null,
    var ferdigstiltTidspunkt: LocalDateTime? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val identer: Long? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val mappeId: Int? = null,
    val oppgavetype: String? = null,
    val opprettetAv: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    var opprettetTidspunkt: ZonedDateTime? = null,
    val orgnr: String? = null,
    val prioritet: String? = null,
    val saksreferanse: String? = null,
    val samhandlernr: String? = null,
    val status: String? = null,
    val tema: String? = null,
    val temagruppe: String? = null,
    val tildeltEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    var versjon: Int = 1,
    var metadata: Map<OppgaveMetadataKey, String>? = null
)

enum class OppgaveMetadataKey {
    NORM_DATO, SOKNAD_ID, REVURDERINGSTYPE, KRAV_ID, MOTTATT_DATO, EKSTERN_HENVENDELSE_ID, SKANNET_DATO, RINA_SAKID, HJEMMEL
}

data class OppgavelisteRespons(
    val antallTreffTotalt: Int,
    val oppgaver: Collection<Oppgave>
)
