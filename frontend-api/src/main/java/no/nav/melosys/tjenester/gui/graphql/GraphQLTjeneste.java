package no.nav.melosys.tjenester.gui.graphql;

import java.util.Map;
import java.util.Objects;

import graphql.ExecutionInput;
import graphql.GraphQL;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import static java.util.Collections.emptyMap;

@Protected
@Controller
public class GraphQLTjeneste {
    private final GraphQL graphQL;

    public GraphQLTjeneste(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @PostMapping(path = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Map<String, Object> graphql(@RequestBody GraphQLRequest request) {
        String query = Objects.nonNull(request.query()) ? request.query() : "";
        Map<String, Object> variables = Objects.nonNull(request.variables()) ? request.variables() : emptyMap();

        var executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .operationName(request.operationName())
            .variables(variables)
            .build();

        var executionResult = graphQL.execute(executionInput);
        return executionResult.toSpecification();
    }
}
