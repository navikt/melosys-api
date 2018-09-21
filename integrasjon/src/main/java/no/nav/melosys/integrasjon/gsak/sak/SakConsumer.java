package no.nav.melosys.integrasjon.gsak.sak;

import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.felles.GsakConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakSearchRequest;

public interface SakConsumer extends GsakConsumer {

    SakDto hentSak(Long id);

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest);

    SakDto opprettSak(SakDto sakDto) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException;
}
