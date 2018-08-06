package no.nav.melosys.integrasjon.gsak.oppgave;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.FeilResponseDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSvar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

@Component
public class OppgaveConsumerImpl implements RestConsumer, OppgaveConsumer {

    private static final Logger log = LoggerFactory.getLogger(OppgaveConsumerImpl.class);

    private final WebTarget target;

    @Value("${OppgaveAPI_v1.url}")
    private String endpointUrl;

    public OppgaveConsumerImpl() {
        try {
            SSLContext sslContext = SSLContext.getDefault();
            Client client = ClientBuilder.newBuilder().sslContext(sslContext).build();
            target = client.register(JacksonJsonProvider.class).target(endpointUrl);
        } catch (NoSuchAlgorithmException e) {
            log.error("Feilet under oppsett av integrasjon mot Sak API", e);
            throw new IntegrasjonException("Feilet under oppsett av integrasjon mot Oppgave API");
        }
    }

    @Override
    public OppgaveDto hentOppgave(String oppgaveId) {
        return target
            .path(oppgaveId)
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .get(OppgaveDto.class);
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) {
        if (oppgaveSearchRequest.getAktørId() != null) {
            target.queryParam("aktoerId", oppgaveSearchRequest.getAktørId());
        }

        target.queryParam("enhetId", oppgaveSearchRequest.getEnhetId())
            .queryParam("sorteringsfelt", oppgaveSearchRequest.getSorteringsfelt())
            .queryParam("tilordnetRessurs", oppgaveSearchRequest.getTilordnetRessurs());

        leggTilQueryParamSomArray("team", oppgaveSearchRequest.getTema());
        leggTilQueryParamSomArray("oppgavetype", oppgaveSearchRequest.getOppgavetype());
        leggTilQueryParamSomArray("behandlingstype", oppgaveSearchRequest.getBehandlingstype());

        OppgaveSvar oppgaveSvar = target.request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .get(OppgaveSvar.class);
        return oppgaveSvar.getOppgaver();
    }

    // Eksempel: https://oppgave.nais.preprod.local/api/v1/oppgaver?tema=MED&tema=MEL
    private void leggTilQueryParamSomArray(String key, String[] param) {
        Arrays.stream(param).forEach(s -> target.queryParam(key, s));
    }

    @Override
    public void oppdaterOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        try (Response response = target.path(request.getId())
            .request(MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .put(Entity.json(request))) {
            håndterFeil(response);
        }
    }

    @Override
    public String opprettOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        try (Response response = target
            .path(request.getId())
            .request(MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getBearer())
            .post(Entity.json(request))) {
            if( response.getStatus() == 201 ) { // Oppgaven opprettet
                OppgaveDto oppgaveDto = response.readEntity(OppgaveDto.class);
                return oppgaveDto.getId();
            }
            håndterFeil(response);
        }
        throw new TekniskException("Uventet feil har oppstått i OpprettOppgave");
    }

    private void håndterFeil(Response response) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        if (response == null) {
            throw new TekniskException("Ingen response fra GSAK Oppgave REST Tjeneste");
        }
        FeilResponseDto feilResponseDto = response.readEntity(FeilResponseDto.class);
        log.error("Uuid={}, Response Kode={}, Feilmelding={}", feilResponseDto.getUuid(), response.getStatus(), feilResponseDto.getFeilmelding());
        if (response.getStatus() == 401 || response.getStatus() == 403) {
            throw new SikkerhetsbegrensningException(feilResponseDto.getFeilmelding());
        } else if (Response.Status.Family.familyOf(response.getStatus()) == CLIENT_ERROR) {
            throw new FunksjonellException(feilResponseDto.getFeilmelding());
        } else if (Response.Status.Family.familyOf(response.getStatus()) == SERVER_ERROR) {
            throw new TekniskException(feilResponseDto.getFeilmelding());
        }
    }
}
