package no.nav.melosys.integrasjon.pdl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLError;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLRequest;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse;
import no.nav.melosys.integrasjon.pdl.dto.identer.HentIdenterResponse;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import no.nav.melosys.integrasjon.pdl.dto.person.HentPersonResponse;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.pdl.dto.identer.Query.HENT_IDENTER_QUERY;
import static no.nav.melosys.integrasjon.pdl.dto.person.Query.HENT_PERSON_QUERY;

public class PDLConsumerImpl implements PDLConsumer {
    private static final Logger log = LoggerFactory.getLogger(PDLConsumerImpl.class);
    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();
    private static final String CALL_ID = "Nav-Call-Id";
    private static final String IDENT = "ident";

    private final WebClient webClient;

    public PDLConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Identliste hentIdenter(String ident) throws IkkeFunnetException, IntegrasjonException {
        GraphQLRequest request = new GraphQLRequest(HENT_IDENTER_QUERY, Map.of(IDENT, ident));

        GraphQLResponse<HentIdenterResponse> response = webClient.post()
            .header(CALL_ID, UUID.randomUUID().toString())
            .bodyValue(request)
            .retrieve().bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentIdenterResponse>>() {})
            .block();

        håndterFeil(response);
        return response.data().hentIdenter();
    }

    @Override
    public Person hentPerson(String ident) throws IkkeFunnetException, IntegrasjonException {
        GraphQLRequest request = new GraphQLRequest(HENT_PERSON_QUERY, Map.of(IDENT, ident));

        GraphQLResponse<HentPersonResponse> response = webClient.post()
            .header(CALL_ID, UUID.randomUUID().toString())
            .bodyValue(request)
            .retrieve().bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentPersonResponse>>() {})
            .block();

        håndterFeil(response);
        return response.data().hentPerson();
    }

    private void håndterFeil(GraphQLResponse<?> response) throws IkkeFunnetException, IntegrasjonException {
        if (response == null) {
            throw new IntegrasjonException("Respons fra PDL er null!");
        }
        if (!CollectionUtils.isEmpty(response.errors())) {
            if (response.errors().stream().anyMatch(NOT_FOUND)) {
                throw new IkkeFunnetException("Fant ikke ident i PDL.");
            }
            throw new IntegrasjonException("Kall mot PDL feilet: " + tilJSON(response.errors()));
        }
    }

    private static final Predicate<GraphQLError> NOT_FOUND = e -> e.hasExtension() && e.extensions().hasCode(
        PDLFeilkode.NOT_FOUND);

    private String tilJSON(List<GraphQLError> errors) {
        String graphQLErrorsAsJSON = null;
        try {
            graphQLErrorsAsJSON = JSON_WRITER.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            log.error("GraphQL feil kunne ikke serialiseres: ", e);
        }
        return graphQLErrorsAsJSON;
    }
}
