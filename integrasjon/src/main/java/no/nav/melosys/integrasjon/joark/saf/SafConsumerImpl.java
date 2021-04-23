package no.nav.melosys.integrasjon.joark.saf;

import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLError;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLRequest;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse;
import no.nav.melosys.integrasjon.joark.Variantformat;
import no.nav.melosys.integrasjon.joark.saf.dto.FeilResponseSafDto;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.HentJournalpostResponse;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Query;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class SafConsumerImpl implements SafConsumer {
    private static final String CALL_ID = "Nav-Callid";
    private static final String SAF_HENT_DOKUMENT_URL = "/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}";
    private static final String SAF_GRAPHQL_URL = "/graphql";

    private final WebClient webClient;

    public SafConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public byte[] hentDokument(String journalpostID, String dokumentID) {
        return webClient.get()
            .uri(SAF_HENT_DOKUMENT_URL, journalpostID, dokumentID, Variantformat.ARKIV)
            .header(CALL_ID, getCallID())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_PDF_VALUE)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterHttpFeil)
            .bodyToMono(byte[].class)
            .block();
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) {
        GraphQLRequest request = new GraphQLRequest(Query.HENT_JOURNALPOST_QUERY, Map.of(Query.JOURNALPOST_ID, journalpostID));
        GraphQLResponse<HentJournalpostResponse> response = webClient.post()
            .uri(SAF_GRAPHQL_URL)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterHttpFeil)
            .bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentJournalpostResponse>>() {
            })
            .block();

        if (response == null) {
            throw new IntegrasjonException("Ingen respons fra Saf ved henting av journalpost");
        }

        if (!CollectionUtils.isEmpty(response.errors())) {
            throw new IntegrasjonException("Feil ved henting av journalpost: "
                + response.errors().stream().map(GraphQLError::message).collect(Collectors.joining(", ")));
        }

        return response.data().journalpost();
    }

    private Mono<Exception> håndterHttpFeil(ClientResponse clientResponse) {
        final HttpStatus status = clientResponse.statusCode();
        return clientResponse.bodyToMono(FeilResponseSafDto.class)
            .map(FeilResponseSafDto::message)
            .map(feilmelding -> tilException(feilmelding, status));
    }
}
