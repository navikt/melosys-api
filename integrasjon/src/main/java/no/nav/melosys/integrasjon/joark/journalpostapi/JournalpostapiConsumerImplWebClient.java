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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class JournalpostapiConsumerImplWebClient implements JournalpostapiConsumer, JsonRestIntegrasjon {
    private static final Logger log = LoggerFactory.getLogger(JournalpostapiConsumerImpl.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public JournalpostapiConsumerImplWebClient(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest opprettJournalpostRequest, boolean forsøkEndeligJfr) {
        if (log.isInfoEnabled()) {
            log.info("Oppretter journalpost av type {} for sak {}",
                opprettJournalpostRequest.getJournalpostType().name(), opprettJournalpostRequest.getSak() != null ? opprettJournalpostRequest.getSak().getFagsakId() : "ukjent");
        }

        return webClient.post()
            .uri("/journalpost?forsoekFerdigstill={forsoekFerdigstill}", forsøkEndeligJfr)
            .headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()))
            .bodyValue(opprettJournalpostRequest)
            .retrieve()
            .bodyToMono(OpprettJournalpostResponse.class)
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .doOnError(WebClientRequestException.class, this::handleWebClientRequestException)
            .block();

    }

    @Override
    public void oppdaterJournalpost(OppdaterJournalpostRequest oppdaterJournalpostRequest, String journalpostId) {
        if (log.isInfoEnabled()) {
            log.info("Oppdaterer journalpost med id {}", journalpostId);
        }

        webClient.put()
            .uri("/journalpost/{journalpostID}", journalpostId)
            .headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()))
            .bodyValue(oppdaterJournalpostRequest)
            .retrieve()
            .toBodilessEntity()
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .doOnError(WebClientRequestException.class, this::handleWebClientRequestException)
            .block();
    }

    @Override
    public void leggTilLogiskVedlegg(String dokumentInfoId, String tittel) {
        if (log.isInfoEnabled()) {
            log.info("Legger til logisk vedlegg for dokument med id {}", dokumentInfoId);
        }

        LogiskVedleggRequest logiskVedleggRequest = new LogiskVedleggRequest(tittel);

        webClient.post()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/", dokumentInfoId)
            .headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()))
            .bodyValue(logiskVedleggRequest)
            .retrieve()
            .toBodilessEntity()
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .doOnError(WebClientRequestException.class, this::handleWebClientRequestException)
            .block();
    }

    @Override
    public void fjernLogiskeVedlegg(String dokumentInfoId, String logiskVedleggId) {
        if (log.isInfoEnabled()) {
            log.info("Fjerner logisk vedlegg {} for dokument med id {}", logiskVedleggId, dokumentInfoId);
        }

        webClient.delete()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/{logiskVedleggId}", dokumentInfoId, logiskVedleggId)
            .headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()))
            .retrieve()
            .toBodilessEntity()
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .doOnError(WebClientRequestException.class, this::handleWebClientRequestException)
            .block();

    }

    @Override
    public void ferdigstillJournalpost(FerdigstillJournalpostRequest ferdigstillJournalpostRequest, String journalpostId) {
        if (log.isInfoEnabled()) {
            log.info("Ferdigstill journalpost med id {}", journalpostId);
        }

        webClient.patch()
            .uri("/journalpost/{journalpostID}/ferdigstill", journalpostId)
            .headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()))
            .bodyValue(ferdigstillJournalpostRequest)
            .retrieve()
            .toBodilessEntity()
            .doOnError(WebClientResponseException.class, this::handleWebClientResponseException)
            .doOnError(WebClientRequestException.class, this::handleWebClientRequestException)
            .block();
    }

    private void handleWebClientResponseException(WebClientResponseException webClientResponseException) {
        String feilmelding = hentFeilmelding(webClientResponseException);
        if (webClientResponseException.getStatusCode() == UNAUTHORIZED || webClientResponseException.getStatusCode() == FORBIDDEN) {
            throw new SikkerhetsbegrensningException(feilmelding, webClientResponseException);
        } else {
            throw new IntegrasjonException(feilmelding, webClientResponseException);
        }
    }

    private void handleWebClientRequestException(WebClientRequestException webClientRequestException) {
        throw new IntegrasjonException("Ukjent feil mot journalpostapi", webClientRequestException);
    }

    private String hentFeilmelding(WebClientResponseException webClientResponseException) {
        String feilmelding = webClientResponseException.getResponseBodyAsString();
        if (StringUtils.hasText(feilmelding)) return webClientResponseException.getMessage();
        try {
            JsonNode json = objectMapper.readTree(feilmelding).path("message");
            return json.isMissingNode() ? webClientResponseException.getMessage() : json.toString();
        } catch (IOException ex) {
            log.warn("Kunne ikke lese feilmelding fra response", ex);
            return feilmelding;
        }
    }
}
