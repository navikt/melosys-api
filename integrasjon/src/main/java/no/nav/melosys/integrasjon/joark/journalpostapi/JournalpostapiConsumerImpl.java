package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class JournalpostapiConsumerImpl implements JournalpostapiConsumer {
    private static final Logger log = LoggerFactory.getLogger(JournalpostapiConsumerImpl.class);

    private final RestTemplate restTemplate;

    public JournalpostapiConsumerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr) {
        log.info("Oppretter journalpost av type {} for arkivsakid {}",
            request.getJournalpostType().name(), request.getSak() != null ? request.getSak().getArkivsaksnummer() : "ukjent");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("")
            .queryParam("forsoekFerdigstill", forsøkEndeligJfr);

        return restTemplate.postForObject(uriBuilder.toUriString(), new HttpEntity<>(request, headers), OpprettJournalpostResponse.class);
    }
}
