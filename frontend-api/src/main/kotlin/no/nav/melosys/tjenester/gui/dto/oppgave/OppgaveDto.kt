package no.nav.melosys.tjenester.gui.dto.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.oppgave.Oppgave
import java.time.LocalDate
import java.time.ZonedDateTime

data class OppgaveDto(
    val oppgaveID: String,
    val tema: Tema?,
    val oppgavetype: String?,
    val registrertDato: ZonedDateTime?,
    val frist: LocalDate?,
    val sakID: String?,
    val journalpostID: String?
) {
    companion object {
        fun av(oppgave: Oppgave): OppgaveDto = OppgaveDto(
            oppgaveID =  oppgave.oppgaveId,
            tema = oppgave.tema,
            oppgavetype = oppgave.oppgavetype?.beskrivelse ?: "",
            registrertDato = oppgave.opprettetTidspunkt,
            frist = oppgave.fristFerdigstillelse,
            sakID = oppgave.saksnummer,
            journalpostID = oppgave.journalpostId,
        )
    }
}
