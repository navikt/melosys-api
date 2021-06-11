package no.nav.melosys.tjenester.gui.graphql;

import java.util.Map;
import java.util.Objects;

import graphql.ExecutionInput;
import graphql.GraphQL;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Collections.emptyMap;

@Protected
@RestController
public class GraphQLTjeneste {
    private final GraphQL graphQL;
    private final TilgangService tilgangService;

    public GraphQLTjeneste(GraphQL graphQL, TilgangService tilgangService) {
        this.graphQL = graphQL;
        this.tilgangService = tilgangService;
    }

    @PostMapping(path = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> graphql(@RequestBody GraphQLRequest request) {
        String query = Objects.nonNull(request.query()) ? request.query() : "";
        Map<String, Object> variables = Objects.nonNull(request.variables()) ? request.variables() : emptyMap();
        tilgangService.sjekkTilgang(validerOgHentBehandlingID(variables));

        var executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .operationName(request.operationName())
            .variables(variables)
            .build();

        var executionResult = graphQL.execute(executionInput);
        return executionResult.toSpecification();
    }

    private long validerOgHentBehandlingID(Map<String, Object> variables) {
        Object behandlingID = variables.get("behandlingID");
        if (!(behandlingID instanceof Integer)) {
            throw new IllegalArgumentException("behandlingID mangler.");
        } else {
            return Integer.toUnsignedLong((Integer) behandlingID);
        }
    }
}
