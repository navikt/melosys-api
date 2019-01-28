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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.gsak.felles.dto.FeilResponseDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakDto;
import no.nav.melosys.integrasjon.gsak.sak.dto.SakSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SakConsumerImpl implements RestConsumer, SakConsumer {

    private static final Logger log = LoggerFactory.getLogger(SakConsumerImpl.class);

    private static final GenericType<List<SakDto>> sakDtoListType = new GenericType<List<SakDto>>() {};

    private final boolean erSystem;

    private final WebTarget target;

    SakConsumerImpl(String endpointUrl, boolean erSystem) throws IntegrasjonException {
        this.erSystem = erSystem;
        try {
            SSLContext sslContext = SSLContext.getDefault();
            Client client = ClientBuilder.newBuilder().sslContext(sslContext).build();
            target = client.register(JacksonObjectMapperProvider.class).target(endpointUrl);
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
    public SakDto hentSak(Long id) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException {
        try {
            return target
                .path(Long.toString(id))
                .request()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", getCallID())
                .header(HttpHeaders.AUTHORIZATION, getAuth())
                .get(SakDto.class);
        } catch (RuntimeException e) {
            ExceptionMapper.JaxGetRuntimeExTilMelosysEx(e);
            return null; // Død kode
        }
    }

    @Override
    public List<SakDto> finnSaker(SakSearchRequest sakSearchRequest) 
        throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException {
        try {
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
        } catch (RuntimeException e) {
            ExceptionMapper.JaxGetRuntimeExTilMelosysEx(e);
            return null; // Død kode
        }
    }

    @Override
    public SakDto opprettSak(SakDto sakDto) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        sakDto.setOpprettetAv(getUserID());
        try (Response response = target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .post(Entity.json(sakDto))) {
            håndterEvFeil(response);
            return response.readEntity(SakDto.class);
        }
    }

    @Override
    public void håndterEvFeil(Response response) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException {
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) return;
        FeilResponseDto feilResponseDto = response.readEntity(FeilResponseDto.class);
        log.error("Feil oppstod. Uuid={}, Response Kode={}, Feilmelding={}", feilResponseDto.getUuid(), response.getStatus(), feilResponseDto.getFeilmelding());
        statusTilException(response.getStatus(), feilResponseDto.getFeilmelding());
    }
}
