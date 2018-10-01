package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.felles.GsakConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;

public interface OppgaveConsumer extends GsakConsumer {

    OppgaveDto hentOppgave(String oppgaveId) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException;

    List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    void oppdaterOppgave(OppgaveDto request) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException, FunksjonellException;

    String opprettOppgave(OpprettOppgaveDto request) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException, FunksjonellException;
}

