package no.nav.melosys.integrasjon.gsak.oppgave;

import java.security.NoSuchAlgorithmException;
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
import no.nav.melosys.integrasjon.gsak.felles.dto.FeilResponseDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSvar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OppgaveConsumerImpl implements RestConsumer, OppgaveConsumer {

    private static final Logger log = LoggerFactory.getLogger(OppgaveConsumerImpl.class);

    private final boolean erSystem;

    private WebTarget target;

    OppgaveConsumerImpl(String endpointUrl, boolean erSystem) {
        this.erSystem = erSystem;
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
    public boolean isSystem() {
        return erSystem;
    }

    @Override
    public OppgaveDto hentOppgave(String oppgaveId) {
        return target
            .path(oppgaveId)
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .get(OppgaveDto.class);
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) {
        WebTarget lokalTarget = target;
        if (oppgaveSearchRequest.getAktørId() != null) {
            lokalTarget = lokalTarget.queryParam("aktoerId", oppgaveSearchRequest.getAktørId());
        }

        lokalTarget = lokalTarget.queryParam("tildeltEnhetsnr", oppgaveSearchRequest.getTildeltEnhetsnr())
            .queryParam("sorteringsfelt", oppgaveSearchRequest.getSorteringsfelt())
            .queryParam("tilordnetRessurs", oppgaveSearchRequest.getTilordnetRessurs());

        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "tema", oppgaveSearchRequest.getTema());
        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "oppgavetype", oppgaveSearchRequest.getOppgavetype());
        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "behandlingstype", oppgaveSearchRequest.getBehandlingstype());

        OppgaveSvar oppgaveSvar = lokalTarget.request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .get(OppgaveSvar.class);
        return oppgaveSvar.getOppgaver();
    }

    // Eksempel: https://oppgave.nais.preprod.local/api/v1/oppgaver?tema=MED&tema=MEL
    private WebTarget leggTilQueryParamSomArray(WebTarget tempTarget, String key, String[] param) {
        if (param != null) {
            for (String s : param) {
                if (s != null) {
                    tempTarget = tempTarget.queryParam(key, s);
                }
            }
        }
        return tempTarget;
    }

    @Override
    public void oppdaterOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        try (Response response = target.path(request.getId())
            .request(MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .put(Entity.json(request))) {
            if (response.getStatus() != 200) {
                håndterFeil(response);
            }
        }
    }

    @Override
    public String opprettOppgave(OppgaveDto request) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        try (Response response = target
            .path(request.getId())
            .request(MediaType.APPLICATION_JSON)
            .header("X-Correlation-ID", getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .post(Entity.json(request))) {
            if (response.getStatus() == 201) { // Oppgaven opprettet
                OppgaveDto oppgaveDto = response.readEntity(OppgaveDto.class);
                return oppgaveDto.getId();
            }
            håndterFeil(response);
        }
        throw new TekniskException("Uventet feil har oppstått i opprettOppgave");
    }

    @Override
    public void håndterFeil(Response response) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        FeilResponseDto feilResponseDto = response.readEntity(FeilResponseDto.class);
        log.error("Feil oppstod. Uuid={}, Response Kode={}, Feilmelding={}", feilResponseDto.getUuid(), response.getStatus(), feilResponseDto.getFeilmelding());
        statusTilException(response.getStatus(), feilResponseDto.getFeilmelding());
    }
}
