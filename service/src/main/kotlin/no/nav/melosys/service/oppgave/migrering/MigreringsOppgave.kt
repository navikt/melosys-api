package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.PrioritetType
import java.time.LocalDate
import java.time.ZonedDateTime

data class MigreringsOppgave(
    val aktørId: String? = null,
    val orgnr: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val beskrivelse: String? = null,
    val behandlesAvApplikasjon: Fagsystem? = null,
    val opprettetTidspunkt: ZonedDateTime? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val journalpostId: String? = null,
    val oppgaveId: String? = null,
    val oppgavetype: Oppgavetyper? = null,
    val prioritet: PrioritetType? = null,
    val saksnummer: String? = null,
    val tema: Tema? = null,
    val temagruppe: String? = null,
    val tilordnetRessurs: String? = null,
    val tildeltEnhetsnr: String? = null,
    val versjon: Int? = null,
    val aktivDato: LocalDate? = null,
    val status: String? = null
) {
    constructor(oppgave: Oppgave) :
        this(
            oppgave.aktørId,
            oppgave.orgnr,
            oppgave.behandlingstema,
            oppgave.behandlingstype,
            oppgave.beskrivelse,
            oppgave.behandlesAvApplikasjon,
            oppgave.opprettetTidspunkt,
            oppgave.fristFerdigstillelse,
            oppgave.journalpostId,
            oppgave.oppgaveId,
            oppgave.oppgavetype,
            oppgave.prioritet,
            oppgave.saksnummer,
            oppgave.tema,
            oppgave.temagruppe,
            oppgave.tilordnetRessurs,
            oppgave.tildeltEnhetsnr,
            oppgave.versjon,
            oppgave.aktivDato,
            oppgave.status
        )

    fun htmlTableData(): String {
        return """
            <td  style="background-color:LIGHTGREEN">$oppgavetype</td>
            <td>$behandlingstype</td>
            <td>$behandlingstema</td>
            <td>$tilordnetRessurs</td>
            <td>$opprettetTidspunkt</td>
            <td>$fristFerdigstillelse</td>
        """.trimIndent()
    }

}
