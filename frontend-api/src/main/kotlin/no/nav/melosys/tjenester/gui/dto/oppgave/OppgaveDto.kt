package no.nav.melosys.tjenester.gui.dto.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.oppgave.Oppgave
import java.time.LocalDate
import java.time.ZonedDateTime

class OppgaveDto private constructor(var oppgaveID: String) {
    var tema: Tema? = null
    var oppgavetype: String? = null
    var registrertDato: ZonedDateTime? = null
    var frist: LocalDate? = null
    var sakID: String? = null
    var journalpostID: String? = null

    companion object {
        @JvmStatic
        fun av(oppgave: Oppgave): OppgaveDto {
            val oppgaveDto = OppgaveDto(oppgave.oppgaveId)
            oppgaveDto.tema = oppgave.tema
            oppgaveDto.oppgavetype = if (oppgave.oppgavetype != null) oppgave.oppgavetype.beskrivelse else ""
            oppgaveDto.registrertDato = oppgave.opprettetTidspunkt
            oppgaveDto.frist = oppgave.fristFerdigstillelse
            oppgaveDto.sakID = oppgave.saksnummer
            oppgaveDto.journalpostID = oppgave.journalpostId
            return oppgaveDto
        }
    }
}
