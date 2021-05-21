package no.nav.melosys.integrasjon.sak;

import java.util.List;

import no.nav.melosys.integrasjon.felles.FeilHandterer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import no.nav.melosys.integrasjon.sak.dto.SakSearchRequest;

public interface SakConsumer extends FeilHandterer {

    SakDto hentSak(Long id);

    List<SakDto> finnSaker(SakSearchRequest sakSearchRequest);

    SakDto opprettSak(SakDto sakDto);
}
