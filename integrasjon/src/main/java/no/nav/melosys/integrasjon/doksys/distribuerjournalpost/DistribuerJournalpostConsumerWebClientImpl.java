package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import no.nav.melosys.integrasjon.felles.RestErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Retryable
public class DistribuerJournalpostConsumerWebClientImpl extends RestErrorHandler implements DistribuerJournalpostConsumer {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpostConsumerWebClientImpl.class);

    private final WebClient webClient;

    public DistribuerJournalpostConsumerWebClientImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    public DistribuerJournalpostResponse distribuerJournalpost(DistribuerJournalpostRequest request) {
        log.info("Distribuerer journalpost {}", request.getJournalpostId());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return webClient.post()
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DistribuerJournalpostResponse.class)
            .doOnError(WebClientResponseException.class, webClientResponseException -> {
                    throw tilException(
                        webClientResponseException.getResponseBodyAsString(),
                        webClientResponseException.getStatusCode()
                    );
                }
            )
            .doOnError(ex -> {
                throw new IntegrasjonException("Ukjent feil mot distribuerjournalpost", ex);
            })
            .block();
    }

}
