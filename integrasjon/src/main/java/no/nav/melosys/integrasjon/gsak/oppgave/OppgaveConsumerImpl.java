package no.nav.melosys.integrasjon.gsak.oppgave;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider;
import no.nav.melosys.integrasjon.felles.RestClientLoggingFilter;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.gsak.felles.dto.FeilResponseDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSvar;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OppgaveConsumerImpl implements RestConsumer, OppgaveConsumer {

    private static final Logger log = LoggerFactory.getLogger(OppgaveConsumerImpl.class);
    private static final String CORRELATION_ID = "X-Correlation-ID";
    private static final int OPPGAVE_ANTALL_LIMIT = 20;

    private final boolean erSystem;

    private WebTarget target;

    OppgaveConsumerImpl(String endpointUrl, boolean erSystem) throws IntegrasjonException {
        this.erSystem = erSystem;
        try {
            SSLContext sslContext = SSLContext.getDefault();
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
            clientConfig.register(new RestClientLoggingFilter());
            Client client = ClientBuilder.newBuilder().sslContext(sslContext).withConfig(clientConfig).build();
            target = client.register(JacksonObjectMapperProvider.class).target(endpointUrl);
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
    public OppgaveDto hentOppgave(String oppgaveId) throws FunksjonellException, TekniskException {
        try {
            return target
                .path(oppgaveId)
                .request()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(CORRELATION_ID, getCallID())
                .header(HttpHeaders.AUTHORIZATION, getAuth())
                .get(OppgaveDto.class);
        } catch (RuntimeException e) {
            ExceptionMapper.JaxGetRuntimeExTilMelosysEx(e);
            return null; // Død kode
        }
    }

    @Override
    public List<OppgaveDto> hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest) throws FunksjonellException, TekniskException {
        OppgaveSvar førsteOppgaveSvar = hentOppgaveListe(oppgaveSearchRequest, 0);

        List<OppgaveDto> alleOppgavene = new ArrayList<>(førsteOppgaveSvar.getOppgaver());
        int antallTreffTotalt = førsteOppgaveSvar.getAntallTreffTotalt();
        if (antallTreffTotalt > OPPGAVE_ANTALL_LIMIT) {
            for (int i = OPPGAVE_ANTALL_LIMIT; i < antallTreffTotalt; i += OPPGAVE_ANTALL_LIMIT) {
                OppgaveSvar oppgaveSvar = hentOppgaveListe(oppgaveSearchRequest, i);
                alleOppgavene.addAll(oppgaveSvar.getOppgaver());
            }
        }
        return alleOppgavene;
    }

    OppgaveSvar hentOppgaveListe(OppgaveSearchRequest oppgaveSearchRequest, int offset) throws FunksjonellException, TekniskException {
        WebTarget lokalTarget = target;
        if (oppgaveSearchRequest.getAktørId() != null) {
            lokalTarget = lokalTarget.queryParam("aktoerId", oppgaveSearchRequest.getAktørId());
        }

        lokalTarget = lokalTarget.queryParam("tildeltEnhetsnr", oppgaveSearchRequest.getTildeltEnhetsnr())
            .queryParam("tildeltRessurs", oppgaveSearchRequest.getTildeltRessurs())
            .queryParam("sorteringsfelt", oppgaveSearchRequest.getSorteringsfelt())
            .queryParam("tilordnetRessurs", oppgaveSearchRequest.getTilordnetRessurs())
            .queryParam("statuskategori", oppgaveSearchRequest.getStatusKategori())
            .queryParam("behandlesAvApplikasjon", oppgaveSearchRequest.getBehandlesAvApplikasjon())
            .queryParam("limit", OPPGAVE_ANTALL_LIMIT)
            .queryParam("offset", offset)
            .queryParam("behandlingstype", oppgaveSearchRequest.getBehandlingstype())
            .queryParam("behandlingstema", oppgaveSearchRequest.getBehandlingstema());

        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "oppgavetype", oppgaveSearchRequest.getOppgavetype());
        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "saksreferanse", oppgaveSearchRequest.getSaksreferanse());
        lokalTarget = leggTilQueryParamSomArray(lokalTarget, "tema", oppgaveSearchRequest.getTema());


        try {
            return lokalTarget.request()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(CORRELATION_ID, getCallID())
                .header(HttpHeaders.AUTHORIZATION, getAuth())
                .get(OppgaveSvar.class);
        } catch (RuntimeException e) {
            ExceptionMapper.JaxGetRuntimeExTilMelosysEx(e);
            return null; // Død kode
        }
    }

    // Eksempel: https://oppgave.nais.preprod.local/api/v1/oppgaver?tema=MED&tema=MEL
    private WebTarget leggTilQueryParamSomArray(WebTarget target, String key, String[] param) {
        WebTarget tempTarget = target;
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
    public void oppdaterOppgave(OppgaveDto request) throws FunksjonellException, TekniskException {
        try (Response response = target.path(request.getId())
            .request(MediaType.APPLICATION_JSON)
            .header(CORRELATION_ID, getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .put(Entity.json(request))) {
            håndterEvFeil(response);
        } catch (RuntimeException e) { // Kan være ProcessingException
            throw new TekniskException(e);
        }
    }

    @Override
    public String opprettOppgave(OpprettOppgaveDto request) throws FunksjonellException, TekniskException {
        try (Response response = target
            .request(MediaType.APPLICATION_JSON)
            .header(CORRELATION_ID, getCallID())
            .header(HttpHeaders.AUTHORIZATION, getAuth())
            .post(Entity.json(request))) {
            håndterEvFeil(response);
            OppgaveDto oppgaveDto = response.readEntity(OppgaveDto.class);
            return oppgaveDto.getId();
        } catch (RuntimeException e) { // Kan være ProcessingException
            throw new TekniskException(e);
        }
    }

    @Override
    public void håndterEvFeil(Response response) throws FunksjonellException, TekniskException {
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) return;
        FeilResponseDto feilResponseDto = response.readEntity(FeilResponseDto.class);
        log.error("Feil oppstod. Uuid={}, Response Kode={}, Feilmelding={}", feilResponseDto.getUuid(), response.getStatus(), feilResponseDto.getFeilmelding());
        statusTilException(response.getStatus(), feilResponseDto.getFeilmelding());
    }
}
