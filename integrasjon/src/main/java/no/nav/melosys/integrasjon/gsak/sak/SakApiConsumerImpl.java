package no.nav.melosys.integrasjon.gsak.sak;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SakApiConsumerImpl implements RestConsumer, SakApiConsumer {

    private static final Logger log = LoggerFactory.getLogger(SakApiConsumerImpl.class);

    private static final GenericType<List<SakDto>> sakDtoListType = new GenericType<List<SakDto>>() {};

    private final boolean erSystem;

    private final WebTarget target;

    SakApiConsumerImpl(String endpointUrl, boolean erSystem) {
        this.erSystem = erSystem;
        try {
            SSLContext sslContext = SSLContext.getDefault();
            Client client = ClientBuilder.newBuilder().sslContext(sslContext).build();
            target = client.register(JacksonJsonProvider.class).target(endpointUrl);
        } catch (NoSuchAlgorithmException e) {
            log.error("Feilet under oppsett av integrasjon mot Sak API", e);
            throw new IntegrasjonException("Feilet under oppsett av integrasjon mot Sak API");
        }
    }

    @Override
    public boolean isSystem() {
        return erSystem;
    }

    @Override
    public SakDto hentSak(Long id) {
        return target
            .path(Long.toString(id))
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .get(SakDto.class);
    }

    @Override
    public List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) {
        return target
            .queryParam("aktoerId", sakSearchRequest.getAktørId())
            .queryParam("orgnr", sakSearchRequest.getOrgnr())
            .queryParam("applikasjon", sakSearchRequest.getApplikasjon())
            .queryParam("tema", sakSearchRequest.getTema())
            .queryParam("fagsakNr", sakSearchRequest.getFagsakNr())
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .get(sakDtoListType);
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) {
        sakDto.setOpprettetAv(getUserID());
        return target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .post(Entity.entity(sakDto, MediaType.APPLICATION_JSON), SakDto.class);
    }
}
