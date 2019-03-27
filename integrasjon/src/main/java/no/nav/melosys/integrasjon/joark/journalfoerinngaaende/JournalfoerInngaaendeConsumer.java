package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import java.util.Collections;

import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class JournalfoerInngaaendeConsumer {

    private final RestTemplate restTemplate;

    public JournalfoerInngaaendeConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GetJournalpostResponse hentJournalpost(String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException {
        return exchange(String.format("/journalposter/%s", journalpostId), HttpMethod.GET,
            new HttpEntity<>(getHttpHeaders()), GetJournalpostResponse.class);
    }

    public PutJournalpostResponse oppdaterJournalpost(PutJournalpostRequest journalpostRequest, String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException {
        return exchange(String.format("/journalposter/%s", journalpostId), HttpMethod.PUT,
            new HttpEntity<>(journalpostRequest, getHttpHeaders()), PutJournalpostResponse.class);
    }

    public PutDokumentResponse oppdaterDokument(PutDokumentRequest dokumentRequest, String journalpostId, String dokumentId) throws SikkerhetsbegrensningException, IntegrasjonException {
        return exchange(String.format("/journalposter/%s/dokumenter/%s", journalpostId, dokumentId),
            HttpMethod.PUT, new HttpEntity<>(dokumentRequest, getHttpHeaders()), PutDokumentResponse.class);
    }

    public void leggTilLogiskVedlegg(PostLogiskVedleggRequest logiskVedleggRequest, String journalpostId, String dokumentId) throws SikkerhetsbegrensningException, IntegrasjonException {
        exchange(String.format("/journalposter/%s/dokumenter/%s/logiskeVedlegg", journalpostId, dokumentId),
            HttpMethod.POST, new HttpEntity<>(logiskVedleggRequest, getHttpHeaders()), Void.class);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, Class<T> clazz) throws SikkerhetsbegrensningException, IntegrasjonException {
        try {
            return restTemplate.exchange(uri, method, entity, clazz).getBody();
        } catch (RestClientException ex) {
            switch (((HttpStatusCodeException)ex).getStatusCode()) {
                case UNAUTHORIZED:
                case FORBIDDEN:
                    throw new SikkerhetsbegrensningException(ex);
                default:
                    throw new IntegrasjonException(ex);
            }
        }
    }
}
