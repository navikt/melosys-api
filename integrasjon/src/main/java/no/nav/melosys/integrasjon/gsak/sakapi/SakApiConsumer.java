package no.nav.melosys.integrasjon.gsak.sakapi;

import java.util.List;

import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakSearchRequest;

public interface SakApiConsumer {

    SakDto hentSak(Long id);

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest);

    SakDto opprettSak(SakDto sakDto);
}
