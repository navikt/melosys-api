package no.nav.melosys.integrasjon.gsak.sakapi;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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

    private String endpointUrl;

    private Client client;

    public SakApiConsumerImpl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        try {
            SSLContext sslContext = SSLContext.getDefault();
            client = ClientBuilder.newBuilder().sslContext(sslContext).build();
        } catch (NoSuchAlgorithmException e) {
            log.error("Kunne ikke opprette client til integrasjon mot Sak API", e);
            throw new IntegrasjonException("Kunne ikke opprette client til integrasjon mot Sak API");
        }
    }

    @Override
    public SakDto hentSak(Long id) {
        return client.target(endpointUrl).path(Long.toString(id)).request()
            .header("Accept", MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getFromMDC(MDC_CALL_ID))
            .header("Authorization", getBearer())
            .get(SakDto.class);
    }

    @Override
    public List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) {
        return null;
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) {
        return client.target(endpointUrl).request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getFromMDC(MDC_CALL_ID))
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .post(Entity.entity(sakDto, MediaType.APPLICATION_JSON), SakDto.class);
    }

    private String getBearer() {
        return "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
    }
}
