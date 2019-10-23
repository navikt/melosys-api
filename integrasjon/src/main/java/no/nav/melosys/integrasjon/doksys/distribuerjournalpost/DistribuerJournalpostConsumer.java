package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class DistribuerJournalpostConsumer {
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

        return restTemplate.postForObject("", new HttpEntity<>(request, headers), DistribuerJournalpostResponse.class);
    }
}
