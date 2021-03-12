package no.nav.melosys.integrasjon.pdl;

import java.util.Map;

import no.nav.melosys.integrasjon.felles.graphql.GraphQLRequest;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse;
import no.nav.melosys.integrasjon.pdl.dto.identer.HentIdenterResponse;
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.pdl.dto.identer.Query.HENT_IDENTER_QUERY;
import static no.nav.melosys.integrasjon.pdl.dto.identer.Query.IDENT;

public class PDLConsumerImpl implements PDLConsumer {
    private final WebClient webClient;

    public PDLConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Identliste hentIdenter(String ident) {
        GraphQLRequest request = new GraphQLRequest(HENT_IDENTER_QUERY, Map.of(IDENT, ident));

        GraphQLResponse<HentIdenterResponse> response = webClient.post()
            .bodyValue(request)
            .retrieve().bodyToMono(new ParameterizedTypeReference<GraphQLResponse<HentIdenterResponse>>() {})
            .block();

        håndterFeil(response);
        return response.data().hentIdenter();
    }

    private void håndterFeil(Object res) {
        //TODO
    }
}
