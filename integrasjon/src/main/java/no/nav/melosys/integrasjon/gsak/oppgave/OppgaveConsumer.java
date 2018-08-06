package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;

public interface OppgaveConsumer {

    OppgaveDto hentOppgave(String oppgaveId);

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws TekniskException;

    void oppdaterOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException;

    String opprettOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException;
}

