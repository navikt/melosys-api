package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.List;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.*;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface OppgaveConsumer extends RestConsumer {

    OppgaveDto hentOppgave(String oppgaveId);

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest);

    OppgaveDto oppdaterOppgave(OppgaveDto request);

    String opprettOppgave(OpprettOppgaveDto request);

    PatchOppgaveDto patchOppgave(PatchOppgaveDto request);
}

