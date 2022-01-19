package stub;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import no.nav.melosys.integrasjon.sak.dto.SakSearchRequest;

import javax.ws.rs.core.Response;
import java.util.List;

public class SakConsumerStub implements RestConsumer, SakConsumer {

    @Override
    public SakDto hentSak(Long id) {
        return null;
    }

    @Override
    public List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) {
        return null;
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) {
        sakDto.setId((long)(Math.random()*100));
        return sakDto;
    }

    @Override
    public void håndterEvFeil(Response response) {

    }
}
