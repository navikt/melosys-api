package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.joark.saf.dto.FeilResponseSafDto;
import no.nav.melosys.integrasjon.joark.saf.dto.VariantFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class SafConsumerImpl implements SafConsumer {
    private static final String CALL_ID = "Nav-Callid";
    private static final String SAF_HENT_DOKUMENT_URL = "/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}";

    private final WebClient webClient;

    public SafConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public byte[] hentDokument(String journalpostID, String dokumentID) {
        return webClient.get()
            .uri(SAF_HENT_DOKUMENT_URL, journalpostID, dokumentID, VariantFormat.ARKIV)
            .header(CALL_ID, getCallID())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_PDF_VALUE)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(byte[].class)
            .block();
    }

    private Mono<Exception> håndterFeil(ClientResponse clientResponse) {
        final HttpStatus status = clientResponse.statusCode();
        return clientResponse.bodyToMono(FeilResponseSafDto.class)
            .map(FeilResponseSafDto::message)
            .map(feilmelding -> tilException(feilmelding, status));
    }
}
