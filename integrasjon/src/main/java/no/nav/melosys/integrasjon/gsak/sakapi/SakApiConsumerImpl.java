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

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sakapi.dto.SakSearchRequest;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.*;

public class SakApiConsumerImpl implements SakApiConsumer {

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
            .header("Accept", MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getFromMDC(MDC_CALL_ID))
            .header("Authorization", getBearer())
            .get(SakDto.class);
    }

    @Override
    public List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) {
        return target
            .queryParam("aktoerId", sakSearchRequest.getAktoerId())
            .queryParam("orgnr", sakSearchRequest.getOrgnr())
            .queryParam("applikasjon", sakSearchRequest.getApplikasjon())
            .queryParam("tema", sakSearchRequest.getTema())
            .queryParam("fagsakNr", sakSearchRequest.getFagsakNr())
            .request()
            .header("Accept", MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getFromMDC(MDC_CALL_ID))
            .header("Authorization", getBearer())
            .get(sakDtoListType);
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) {
        return target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getFromMDC(MDC_CALL_ID))
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .post(Entity.entity(sakDto, MediaType.APPLICATION_JSON), SakDto.class);
    }

    private String getBearer() {
        return "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
    }
}
