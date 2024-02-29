package no.nav.melosys.tjenester.gui.dto.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.oppgave.Oppgave
import java.time.LocalDate
import java.time.ZonedDateTime

class OppgaveDto private constructor(val oppgaveID: String) {
    var tema: Tema? = null
    var oppgavetype: String? = null
    var registrertDato: ZonedDateTime? = null
    var frist: LocalDate? = null
    var sakID: String? = null
    var journalpostID: String? = null

    companion object {
        @JvmStatic
        fun av(oppgave: Oppgave): OppgaveDto = OppgaveDto(oppgave.oppgaveId).apply {
            tema = oppgave.tema
            oppgavetype = oppgave.oppgavetype?.beskrivelse ?: ""
            registrertDato = oppgave.opprettetTidspunkt
            frist = oppgave.fristFerdigstillelse
            sakID = oppgave.saksnummer
            journalpostID = oppgave.journalpostId
        }
    }
}
