package no.nav.melosys.integrasjon.gsak.sak;

import java.util.List;

import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakSearchRequest;

public interface SakApiConsumer {

    SakDto hentSak(Long id);

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest);

    SakDto opprettSak(SakDto sakDto);
}
