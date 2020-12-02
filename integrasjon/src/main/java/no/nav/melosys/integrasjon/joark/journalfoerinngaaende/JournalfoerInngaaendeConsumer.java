package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class JournalfoerInngaaendeConsumer implements JsonRestIntegrasjon {

    private final RestTemplate restTemplate;

    public JournalfoerInngaaendeConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GetJournalpostResponse hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException, IntegrasjonException {
        return exchange("/journalposter/{journalpostID}", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), GetJournalpostResponse.class, journalpostID);
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, Class<T> clazz, Object... variabler) throws SikkerhetsbegrensningException, IntegrasjonException {
        try {
            return restTemplate.exchange(uri, method, entity, clazz, variabler).getBody();
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
