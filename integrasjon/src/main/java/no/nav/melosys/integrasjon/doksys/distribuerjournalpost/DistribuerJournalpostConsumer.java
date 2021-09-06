package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class DistribuerJournalpostConsumer implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpostConsumer.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DistribuerJournalpostConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public DistribuerJournalpostResponse distribuerJournalpost(DistribuerJournalpostRequest request) {
        log.info("Distribuerer journalpost {}", request.getJournalpostId());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return exchange("", HttpMethod.POST, new HttpEntity<>(request, headers));
    }

    private DistribuerJournalpostResponse exchange(String uri, HttpMethod method, HttpEntity<?> entity) {
        try {
            return restTemplate.exchange(uri, method, entity, DistribuerJournalpostResponse.class).getBody();
        } catch (HttpStatusCodeException ex) {
            String feilmelding = hentFeilmelding(ex);
            throw tilException(feilmelding, ex.getStatusCode());
        } catch (RestClientException ex) {
            throw new IntegrasjonException("Ukjent feil mot distribuerjournalpost", ex);
        }
    }

    private String hentFeilmelding(HttpStatusCodeException e) {
        String feilmelding = e.getResponseBodyAsString();
        if (!StringUtils.hasText(feilmelding)) return e.getMessage();
        try {
            JsonNode json = objectMapper.readTree(feilmelding).path("message");
            return json.isMissingNode() ? e.getMessage() : json.toString();
        } catch (IOException ex) {
            log.warn("Kunne ikke lese feilmelding fra response", ex);
            return feilmelding;
        }
    }
}
