package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import java.util.Collections;

import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
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

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, Class<T> clazz) throws SikkerhetsbegrensningException, IntegrasjonException {
        try {
            return restTemplate.exchange(uri, method, entity, clazz).getBody();
        } catch (HttpStatusCodeException ex) {
                switch (ex.getStatusCode()) {
                    case UNAUTHORIZED:
                    case FORBIDDEN:
                        throw new SikkerhetsbegrensningException(ex);
                    default:
                        throw new IntegrasjonException(ex);
                }
        } catch (RestClientException ex) {
            throw new IntegrasjonException(ex);
        }
    }
}
