package no.nav.melosys.integrasjon.joark.journalpostapi;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class JournalpostapiConsumerImpl implements JournalpostapiConsumer, JsonRestIntegrasjon {
    private static final Logger log = LoggerFactory.getLogger(JournalpostapiConsumerImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JournalpostapiConsumerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr) {
        if (log.isInfoEnabled()) {
            log.info("Oppretter journalpost av type {} for arkivsakid {}",
                request.getJournalpostType().name(), request.getSak() != null ? request.getSak().getArkivsaksnummer() : "ukjent");
        }

        return restTemplate.postForObject("/journalpost?forsoekFerdigstill={forsoekFerdigstill}", new HttpEntity<>(request, getDefaultHeaders()), OpprettJournalpostResponse.class, forsøkEndeligJfr);
    }

    @Override
    public void oppdaterJournalpost(OppdaterJournalpostRequest request, String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException {
        if (log.isInfoEnabled()) {
            log.info("Oppdaterer journalpost med id {}", journalpostId);
        }
        exchange("/journalpost/{journalpostID}", HttpMethod.PUT, new HttpEntity<>(request, getDefaultHeaders()), journalpostId);
    }

    @Override
    public void leggTilLogiskVedlegg(String dokumentInfoId, String tittel) throws SikkerhetsbegrensningException, IntegrasjonException {
        if (log.isInfoEnabled()) {
            log.info("Legger til logisk vedlegg for dokument med id {}", dokumentInfoId);
        }

        LogiskVedleggRequest request = new LogiskVedleggRequest(tittel);
        exchange("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/", HttpMethod.POST, new HttpEntity<>(request, getDefaultHeaders()), dokumentInfoId);
    }

    @Override
    public void fjernLogiskeVedlegg(String dokumentInfoId, String logiskVedleggId) throws SikkerhetsbegrensningException, IntegrasjonException {
        if (log.isInfoEnabled()) {
            log.info("Fjerner logisk vedlegg {} for dokument med id {}", logiskVedleggId, dokumentInfoId);
        }

        exchange("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/{logiskVedleggId}", HttpMethod.DELETE, new HttpEntity<>(getDefaultHeaders()), dokumentInfoId, logiskVedleggId);
    }

    @Override
    public void ferdigstillJournalpost(FerdigstillJournalpostRequest request, String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException {
        if (log.isInfoEnabled()) {
            log.info("Ferdigstill journalpost med id {}", journalpostId);
        }
        exchange("/journalpost/{journalpostID}/ferdigstill", HttpMethod.PATCH, new HttpEntity<>(request, getDefaultHeaders()), journalpostId);
    }

    private void exchange(String uri, HttpMethod method, HttpEntity<?> entity, Object... variabler) throws SikkerhetsbegrensningException, IntegrasjonException {
        try {
            restTemplate.exchange(uri, method, entity, Void.class, variabler).getBody();
        } catch (HttpStatusCodeException ex) {
            String feilmelding = hentFeilmelding(ex);
            switch (ex.getStatusCode()) {
                case UNAUTHORIZED:
                case FORBIDDEN:
                    throw new SikkerhetsbegrensningException(feilmelding, ex);
                default:
                    throw new IntegrasjonException(feilmelding, ex);
            }
        } catch (RestClientException ex) {
            throw new IntegrasjonException("Ukjent feil mot journalpostapi", ex);
        }
    }

    private String hentFeilmelding(HttpStatusCodeException e) {
        String feilmelding = e.getResponseBodyAsString();
        if (StringUtils.isEmpty(feilmelding)) return e.getMessage();
        try {
            JsonNode json = objectMapper.readTree(feilmelding).path("message");
            return json.isMissingNode() ? e.getMessage() : json.toString();
        } catch (IOException ex) {
            log.warn("Kunne ikke lese feilmelding fra response", ex);
            return feilmelding;
        }
    }
}
