package no.nav.melosys.integrasjon.gsak.sakapi;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SakApiConsumerImpl implements RestConsumer, SakApiConsumer {

    private static final Logger log = LoggerFactory.getLogger(SakApiConsumerImpl.class);

    private final GenericType<List<SakDto>> sakDtoListType = new GenericType<List<SakDto>>() {};

    private final WebTarget target;

    public SakApiConsumerImpl(String endpointUrl) {
        try {
            SSLContext sslContext = SSLContext.getDefault();
            target = ClientBuilder.newBuilder().sslContext(sslContext).build().target(endpointUrl);
        } catch (NoSuchAlgorithmException e) {
            log.error("Feilet under oppsett av integrasjon mot Sak API", e);
            throw new IntegrasjonException("Feilet under oppsett av integrasjon mot Sak API");
        }
    }

    @Override
    public SakDto hentSak(Long id) {
        return target
            .path(Long.toString(id))
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
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
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .get(sakDtoListType);
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) {
        sakDto.setOpprettetAv(getUserID());
        return target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .post(Entity.entity(sakDto, MediaType.APPLICATION_JSON), SakDto.class);
    }

}
