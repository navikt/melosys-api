package no.nav.melosys.integrasjon.sak;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.FeilHandterer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import no.nav.melosys.integrasjon.sak.dto.SakSearchRequest;

public interface SakConsumer extends FeilHandterer {

    SakDto hentSak(Long id) throws FunksjonellException, TekniskException;

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) throws FunksjonellException, TekniskException;

    SakDto opprettSak(SakDto sakDto) throws FunksjonellException, TekniskException;
}
