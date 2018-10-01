package no.nav.melosys.integrasjon.gsak.sak;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.felles.GsakConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakSearchRequest;

public interface SakConsumer extends GsakConsumer {

    SakDto hentSak(Long id) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException;

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException;

    SakDto opprettSak(SakDto sakDto) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException;
}
