package no.nav.melosys.tjenester.gui.dto.oppgave;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;

public class OppgaveDto {
    public String oppgaveID;
    public Tema tema;
    public String oppgavetype;
    public ZonedDateTime registrertDato;
    public LocalDate frist;
    public String sakID;
    public String journalpostID;

    private OppgaveDto(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public static OppgaveDto av(Oppgave oppgave) {
        OppgaveDto oppgaveDto = new OppgaveDto(oppgave.getOppgaveId());
        oppgaveDto.tema = oppgave.getTema();
        oppgaveDto.oppgavetype = oppgave.getOppgavetype() != null ? oppgave.getOppgavetype().getBeskrivelse() : "";
        oppgaveDto.registrertDato = oppgave.getOpprettetTidspunkt();
        oppgaveDto.frist = oppgave.getFristFerdigstillelse();
        oppgaveDto.sakID = oppgave.getSaksnummer();
        oppgaveDto.journalpostID = oppgave.getJournalpostId();
        return oppgaveDto;
    }
}
