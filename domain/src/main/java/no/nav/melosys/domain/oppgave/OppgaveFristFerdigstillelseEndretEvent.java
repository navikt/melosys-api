package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;

import org.springframework.context.ApplicationEvent;

public class OppgaveFristFerdigstillelseEndretEvent extends ApplicationEvent {
    private final String oppgaveId;
    private final LocalDate fristFerdigstillelse;

    public OppgaveFristFerdigstillelseEndretEvent(String oppgaveId, LocalDate fristFerdigstillelse) {
        super(oppgaveId);
        this.oppgaveId = oppgaveId;
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }
}
