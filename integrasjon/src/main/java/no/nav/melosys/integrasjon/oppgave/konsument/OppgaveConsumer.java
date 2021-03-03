package no.nav.melosys.integrasjon.oppgave.konsument;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;

public interface OppgaveConsumer extends RestConsumer {

    OppgaveDto hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException;

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws FunksjonellException, TekniskException;

    OppgaveDto oppdaterOppgave(OppgaveDto request) throws FunksjonellException, TekniskException;

    String opprettOppgave(OpprettOppgaveDto request) throws FunksjonellException, TekniskException;
}

