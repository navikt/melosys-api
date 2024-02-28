package no.nav.melosys.tjenester.gui.dto.oppgave;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;

public class OppgaveDto {
    private String oppgaveID;
    private Tema tema;
    private String oppgavetype;
    private ZonedDateTime registrertDato;
    private LocalDate frist;
    private String sakID;
    private String journalpostID;

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public Tema getTema() {
        return tema;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public ZonedDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(ZonedDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public void setFrist(LocalDate frist) {
        this.frist = frist;
    }

    public String getSakID() {
        return sakID;
    }

    public void setSakID(String sakID) {
        this.sakID = sakID;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

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
