package no.nav.melosys.service.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveFristFerdigstillelseEndretEvent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OppgaveEventListener {

    private final OppgaveService oppgaveService;

    public OppgaveEventListener(OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @TransactionalEventListener
    @Async
    public void fristFerdigstillelseEndret(OppgaveFristFerdigstillelseEndretEvent oppgaveFristFerdigstillelseEndretEvent) throws TekniskException, FunksjonellException {
        String oppgaveId = oppgaveFristFerdigstillelseEndretEvent.getOppgaveId();
        LocalDate nyFrist = oppgaveFristFerdigstillelseEndretEvent.getFristFerdigstillelse();

        oppgaveService.oppdaterOppgave(oppgaveId, OppgaveOppdatering.builder().fristFerdigstillelse(nyFrist).build());
    }
}
