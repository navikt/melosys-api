package no.nav.melosys.integrasjon.pdl;

import java.util.Collection;
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
import no.nav.melosys.integrasjon.pdl.dto.person.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.pdl.dto.identer.Query.HENT_IDENTER_QUERY;
import static no.nav.melosys.integrasjon.pdl.dto.person.Query.*;

public class PDLConsumerImpl implements PDLConsumer {
    private static final Logger log = LoggerFactory.getLogger(PDLConsumerImpl.class);
    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();
    private static final String CALL_ID = "Nav-Call-Id";
    private static final String HISTORIKK = "historikk";
    private static final String IDENT = "ident";

    private final WebClient webClient;

    public PDLConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    @Retryable
    public Identliste hentIdenter(String ident) {
        var graphQLRequest = new GraphQLRequest(HENT_IDENTER_QUERY, Map.of(IDENT, ident));

        GraphQLResponse<HentIdenterResponse> response = webClient.post()
            .header(CALL_ID, UUID.randomUUID().toString())
            .bodyValue(graphQLRequest)
            .retrieve().bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentIdenterResponse>>() {})
            .block();

        håndterFeil(response);
        return response.data().hentIdenter();
    }

    @Override
    @Retryable
    public Person hentPerson(String ident) {
        return hentPersondata(HENT_PERSON_QUERY, ident, false);
    }

    @Override
    @Retryable
    public Person hentPersonMedHistorikk(String ident, boolean innsynn) {
        return hentPersondata(HENT_PERSON_HISTORIKK_QUERY, ident, true);
    }

    @Override
    @Retryable
    public Collection<Adressebeskyttelse> hentAdressebeskyttelser(String ident) {
        return hentPersondata(HENT_ADRESSEBESKYTTELSE_QUERY, ident, false).adressebeskyttelse();
    }

    @Override
    @Retryable
    public Collection<Navn> hentNavn(String ident) {
        return hentPersondata(HENT_SAMMENSATT_NAVN_QUERY, ident, false).navn();
    }

    @Override
    @Retryable
    public Collection<Statsborgerskap> hentStatsborgerskap(String ident) {
        return hentPersondata(HENT_STATSBORGERSKAP_QUERY, ident, true).statsborgerskap();
    }

    private Person hentPersondata(String query, String ident, boolean historikk) throws IkkeFunnetException {
        var graphQLRequest = new GraphQLRequest(query, Map.of(HISTORIKK, historikk, IDENT, ident));

        GraphQLResponse<HentPersonResponse> response = webClient.post()
            .header(CALL_ID, UUID.randomUUID().toString())
            .bodyValue(graphQLRequest)
            .retrieve().bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentPersonResponse>>() {})
            .block();

        håndterFeil(response);
        return response.data().hentPerson();
    }

    private void håndterFeil(GraphQLResponse<?> response) {
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
