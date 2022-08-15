package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.List;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface OppgaveConsumer extends RestConsumer {

    OppgaveDto hentOppgave(String oppgaveId);

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest);

    OppgaveDto oppdaterOppgave(OppgaveDto request);

    String opprettOppgave(OpprettOppgaveDto request);

    OppgaveDto patchOppgave(OppgaveDto request);
}

