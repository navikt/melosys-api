package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import no.nav.melosys.integrasjon.felles.RestErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Retryable
public class DistribuerJournalpostConsumer extends RestErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpostConsumer.class);

    private final RestTemplate restTemplate;
    public DistribuerJournalpostConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            throw tilException(ex);
        } catch (RestClientException ex) {
            throw new IntegrasjonException("Ukjent feil mot distribuerjournalpost", ex);
        }
    }
}
