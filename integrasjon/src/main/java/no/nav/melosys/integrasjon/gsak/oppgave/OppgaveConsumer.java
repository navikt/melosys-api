package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.felles.GsakConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;

public interface OppgaveConsumer extends GsakConsumer {

    OppgaveDto hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException;

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws FunksjonellException, TekniskException;

    void oppdaterOppgave(OppgaveDto request) throws FunksjonellException, TekniskException;

    String opprettOppgave(OpprettOppgaveDto request) throws FunksjonellException, TekniskException;
}

